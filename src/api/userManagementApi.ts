import { fetchWithAuth } from './authApi';
export interface ManagedUser { id:number; username:string; role:string }
export interface CreateUser { username:string; password:string; role:string }
async function request<T>(path='',options?:RequestInit):Promise<T> {
 const response=await fetchWithAuth(`/api/v1/admin/users${path}`,options);
 if(!response.ok) {const error=await response.json().catch(()=>null);throw new Error(error?.message||`HTTP ${response.status}`);}
 return response.status===204 ? undefined as T : response.json();
}
export const listUsers=()=>request<ManagedUser[]>();
export const createUser=(data:CreateUser)=>request<ManagedUser>('',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
export const changeUserRole=(user:ManagedUser,role:string)=>request<ManagedUser>(`/${user.id}/role`,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({role,expectedRole:user.role})});
export function validNewUser(data:CreateUser) {
 return /^[A-Za-z0-9_.-]{3,32}$/.test(data.username)&&validPassword(data.password)&&!!data.role;
}

export const deleteUser=(user:ManagedUser)=>request<void>(`/${user.id}`,{method:'DELETE'});

export const validPassword=(password:string)=>password.length>=6&&new TextEncoder().encode(password).length<=72;
export const changeUserPassword=(user:ManagedUser,password:string)=>request<void>(`/${user.id}/password`,{method:"PATCH",headers:{"Content-Type":"application/json"},body:JSON.stringify({password})});
