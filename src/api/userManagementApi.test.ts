import {afterEach,expect,it,vi} from 'vitest';
import {deleteUser,changeUserRole,createUser,listUsers,validNewUser} from './userManagementApi';
import {fetchWithAuth} from './authApi';
vi.mock('./authApi',()=>({fetchWithAuth:vi.fn()}));
afterEach(()=>vi.resetAllMocks());
it('validates usernames and BCrypt byte limit',()=>{expect(validNewUser({username:'tester',password:'long-enough-password',role:'ROLE_TESTER'})).toBe(true);for(const user of [{username:'x',password:'long-enough-password',role:'ROLE_USER'},{username:'valid',password:'short',role:'ROLE_USER'},{username:'valid',password:'я'.repeat(37),role:'ROLE_USER'}])expect(validNewUser(user)).toBe(false);});
it('sends expected role to detect concurrent changes',async()=>{vi.mocked(fetchWithAuth).mockResolvedValue(new Response('{"id":7,"username":"test","role":"ROLE_USER"}',{status:200}));await changeUserRole({id:7,username:'test',role:'ROLE_READONLY'},'ROLE_USER');expect(fetchWithAuth).toHaveBeenCalledWith('/api/v1/admin/users/7/role',expect.objectContaining({method:'PATCH',body:JSON.stringify({role:'ROLE_USER',expectedRole:'ROLE_READONLY'})}));});
it('surfaces server rejection instead of reporting successful creation',async()=>{vi.mocked(fetchWithAuth).mockResolvedValue(new Response('{"message":"duplicate"}',{status:409}));await expect(createUser({username:'tester',password:'long-enough-password',role:'ROLE_TESTER'})).rejects.toThrow('duplicate');});
it('returns the user list from the authenticated endpoint',async()=>{vi.mocked(fetchWithAuth).mockResolvedValue(new Response('[]',{status:200}));expect(await listUsers()).toEqual([]);});

it('accepts successful deletion with an empty response',async()=>{
 vi.mocked(fetchWithAuth).mockResolvedValue(new Response(null,{status:204}));
 await expect(deleteUser({id:7,username:'test',role:'ROLE_USER'})).resolves.toBeUndefined();
 expect(fetchWithAuth).toHaveBeenCalledWith('/api/v1/admin/users/7',{method:'DELETE'});
});
it('shows rejected deletion as an error',async()=>{
 vi.mocked(fetchWithAuth).mockResolvedValue(new Response('{"message":"denied"}',{status:403}));
 await expect(deleteUser({id:7,username:'test',role:'ROLE_USER'})).rejects.toThrow('denied');
});
