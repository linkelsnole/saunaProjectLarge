import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Users, Plus, Search, Check, RefreshCw, Crown, UserRound, BookOpen, FlaskConical, SlidersHorizontal, MoreHorizontal, ArrowLeft, Eye, EyeOff, Info, LockKeyhole, X } from 'lucide-react';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/shared/ui/dialog';
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/ui/popover';
import { useAuth } from '@/contexts/AuthContext';
import { listUsers, createUser, changeUserRole, deleteUser, validNewUser, type ManagedUser } from '@/api/userManagementApi';

const roles = ['ROLE_ADMIN', 'ROLE_USER', 'ROLE_READONLY', 'ROLE_TESTER', 'ROLE_MGM'];
const roleIcons: Record<string, typeof Users> = { ROLE_ADMIN: Crown, ROLE_USER: UserRound, ROLE_READONLY: BookOpen, ROLE_TESTER: FlaskConical, ROLE_MGM: SlidersHorizontal, ROLE_CONFIG: SlidersHorizontal };
const button = 'inline-flex min-h-10 [&_svg]:shrink-0 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition duration-150 motion-reduce:transition-none disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2';
const primary = `${button} border border-zinc-950 bg-zinc-950 text-white shadow-sm hover:bg-zinc-800 active:scale-[.98] motion-reduce:transform-none`;
const secondary = `${button} border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50`;
const input = 'min-h-11 w-full min-w-0 rounded-xl border border-zinc-200 bg-white px-3.5 py-2.5 text-sm text-zinc-950 outline-none transition duration-150 placeholder:text-zinc-400 focus:border-zinc-500 focus:ring-2 focus:ring-zinc-100 disabled:opacity-50 motion-reduce:transition-none';
const panel = 'rounded-2xl border-zinc-200 bg-white p-2 shadow-[0_12px_40px_-10px_rgba(0,0,0,0.22)] duration-150 motion-reduce:animate-none';
const emptyForm = () => ({ username: '', password: '', role: 'ROLE_READONLY' });

function RoleBadge({ role }: { role: string }) {
 const { t } = useTranslation();
 const Icon = roleIcons[role] ?? UserRound;
 return <span className={`inline-flex max-w-full items-center gap-2 rounded-full px-3 py-1.5 text-xs font-medium ${role === 'ROLE_ADMIN' ? 'bg-zinc-800 text-white' : 'bg-zinc-100 text-zinc-800'}`}><Icon size={14} className="shrink-0"/><span className="truncate">{t(`users.roles.${role}`, { defaultValue: role })}</span></span>;
}

function RoleChoices({ value, onChange, disabled, compact = false, name }: { value: string; onChange: (role: string) => void; disabled: boolean; compact?: boolean; name: string }) {
 const { t } = useTranslation();
 return <fieldset disabled={disabled} className={compact ? 'space-y-1' : 'grid grid-cols-1 gap-2.5 min-[480px]:grid-cols-2'}>
  <legend className="sr-only">{t('users.role')}</legend>
  {roles.map(role => {
   const Icon = roleIcons[role]; const selected = value === role;
   return <label key={role} className={`relative flex cursor-pointer items-start gap-3 transition-colors duration-150 motion-reduce:transition-none ${compact ? 'rounded-xl p-3' : 'rounded-xl border p-3.5 last:col-span-full'} ${selected ? compact ? 'bg-zinc-100' : 'border-zinc-700 bg-zinc-50 shadow-[0_0_0_1px_#3f3f46]' : compact ? 'hover:bg-zinc-50' : 'border-zinc-200 hover:border-zinc-400'} ${disabled ? 'pointer-events-none opacity-50' : ''}`}>
    <input className="peer sr-only" type="radio" name={name} value={role} checked={selected} onChange={() => onChange(role)}/>
    <span className="pointer-events-none absolute inset-0 rounded-xl peer-focus-visible:ring-2 peer-focus-visible:ring-zinc-500 peer-focus-visible:ring-offset-2"/>
    <span className={`flex shrink-0 items-center justify-center ${compact ? 'h-9 w-9 rounded-full bg-zinc-50' : 'pt-0.5'}`}><Icon size={18}/></span>
    <span className="min-w-0 flex-1"><span className="block text-[13px] font-medium text-zinc-950">{t(`users.roles.${role}`)}</span><span className="mt-1 block text-xs leading-relaxed text-zinc-500">{t(`users.hints.${role}`)}</span></span>
    {compact ? <Check size={17} className={`mt-1 shrink-0 ${selected ? 'text-zinc-950' : 'invisible'}`}/> : <span className={`mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full border ${selected ? 'border-zinc-950 bg-zinc-950' : 'border-zinc-300 bg-white'}`}>{selected && <span className="h-1.5 w-1.5 rounded-full bg-white"/>}</span>}
   </label>;
  })}
 </fieldset>;
}

function UserRow({ user, self, fresh, onChanged, onDeleted, setMutationBusy }: { user: ManagedUser; self: boolean; fresh: boolean; onChanged: (user: ManagedUser) => void; onDeleted: (id: number) => void; setMutationBusy: (value: boolean) => void }) {
 const { t } = useTranslation(); const tr = (key: string) => t(`users.${key}`);
 const [confirmDelete, setConfirmDelete] = useState(false);
 const [open, setOpen] = useState(false); const [role, setRole] = useState(user.role); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
 async function remove() {
  if (busy) return;
  setBusy(true);setMutationBusy(true);setError('');
  try {await deleteUser(user);onDeleted(user.id);toast.success(tr('deleted'));}
  catch(e){setError(e instanceof Error?e.message:String(e));}
  finally{setBusy(false);setMutationBusy(false);}
 }
 async function save() {
  if (busy || role === user.role) return;
  setBusy(true); setMutationBusy(true); setError('');
  try { const updated = await changeUserRole(user, role); onChanged(updated); setOpen(false); toast.success(tr('roleSaved')); }
  catch (e) { setError(e instanceof Error ? e.message : String(e)); }
  finally { setBusy(false); setMutationBusy(false); }
 }
 return <tr className={`border-b border-zinc-100 last:border-0 transition-colors duration-150 motion-reduce:transition-none ${fresh ? 'bg-emerald-50/80' : 'hover:bg-zinc-50/60'}`}>
  <td className="py-4 pl-3 pr-2 sm:pl-4"><div className="flex items-center gap-3"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-zinc-100 text-xs font-semibold text-zinc-800">{user.username.slice(0, 2).toUpperCase()}</span><div className="min-w-0"><p className="break-all text-sm font-medium text-zinc-950">{user.username}</p><p className="mt-0.5 text-xs text-zinc-500">{self ? tr('you') : `#${user.id}`}{fresh && <span className="ml-2 rounded-full bg-emerald-100 px-1.5 py-0.5 text-[10px] text-emerald-700">{tr('newBadge')}</span>}</p><div className="mt-2 sm:hidden"><RoleBadge role={user.role}/></div></div></div></td>
  <td className="hidden py-4 px-3 sm:table-cell"><RoleBadge role={user.role}/></td>
  <td className="py-4 pl-2 pr-3 text-right sm:pr-4">{self ? <span className="inline-flex h-9 w-9 items-center justify-center text-zinc-300" title={tr('selfLocked')}><LockKeyhole size={15}/><span className="sr-only">{tr('selfLocked')}</span></span> : <Popover modal open={open} onOpenChange={next => { if (!busy) { setOpen(next); setConfirmDelete(false); setRole(user.role); setError(''); } }}><PopoverTrigger asChild><button className={`${secondary} min-h-9 h-9 w-9 !px-0`} aria-label={`${tr('changeRole')} ${user.username}`}><MoreHorizontal size={18}/></button></PopoverTrigger><PopoverContent align="end" sideOffset={8} collisionPadding={16} sticky="always" className={`${panel} flex w-[min(340px,calc(100vw-2rem))] flex-col overflow-hidden`} style={{ maxHeight: 'min(620px, calc(100dvh - 32px), var(--radix-popover-content-available-height, 620px))' }} onEscapeKeyDown={e => { if (busy) e.preventDefault(); }} onInteractOutside={e => { if (busy) e.preventDefault(); }}>
   {confirmDelete ? <div className="space-y-3 p-3"><h3 className="font-semibold">{tr('deleteTitle')} {user.username}?</h3><p className="text-sm text-zinc-500">{tr('deleteHint')}</p>{error && <p role="alert" className="text-sm text-rose-600">{error}</p>}<div className="flex gap-2"><button className={secondary} disabled={busy} onClick={()=>setConfirmDelete(false)}>{tr('cancel')}</button><button className={primary} disabled={busy} onClick={()=>void remove()}>{tr('delete')}</button></div></div> : <>
   <div className="shrink-0 px-3 pt-2 pb-3"><h3 className="text-sm font-semibold">{tr('chooseRole')}</h3><p className="mt-1 truncate text-xs text-zinc-500">{user.username}</p></div>
   <div className="min-h-0 overflow-y-auto overscroll-contain px-1 py-1 [scrollbar-gutter:stable]"><RoleChoices name={`role-${user.id}`} compact value={role} onChange={setRole} disabled={busy}/></div>
   {error && <p role="alert" className="px-3 py-2 text-xs text-rose-600">{error}</p>}
   <div className="shrink-0 border-t border-zinc-100 p-2"><button className={`${primary} w-full`} disabled={busy || role === user.role} onClick={() => void save()}>{busy && <RefreshCw size={15} className="animate-spin motion-reduce:animate-none"/>}{tr('apply')}</button><button className={`${secondary} mt-2 w-full text-rose-600`} disabled={busy} onClick={()=>setConfirmDelete(true)}>{tr('delete')}</button></div></>}
  </PopoverContent></Popover>}</td>
 </tr>;
}

export function UserManagementDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
 const { role } = useAuth(); const [busy, setBusy] = useState(false);
 if (role !== 'ROLE_ADMIN') return null;
 return <Dialog open={open} onOpenChange={next => { if (!busy) onOpenChange(next); }}><DialogContent className="max-h-[90dvh] gap-6 overflow-y-auto rounded-[24px] border-zinc-200 bg-white p-5 text-zinc-950 shadow-[0_24px_80px_-24px_rgba(0,0,0,.3)] sm:max-w-[820px] sm:p-7 duration-200 motion-reduce:animate-none" onEscapeKeyDown={e => { if (busy) e.preventDefault(); }} onInteractOutside={e => { if (busy) e.preventDefault(); }}><UserManagementContent open={open} busy={busy} setBusy={setBusy}/></DialogContent></Dialog>;
}

function UserManagementContent({ open, busy, setBusy }: { open: boolean; busy: boolean; setBusy: (value: boolean) => void }) {
 const { username } = useAuth(); const { t } = useTranslation(); const tr = (key: string) => t(`users.${key}`); const cache = useQueryClient();
 const query = useQuery({ queryKey: ['managed-users'], queryFn: listUsers, enabled: open, staleTime: 0 });
 const [search, setSearch] = useState(''); const [filter, setFilter] = useState('ALL'); const [creating, setCreating] = useState(false); const [error, setError] = useState('');
 const [form, setForm] = useState(emptyForm); const [visiblePassword, setVisiblePassword] = useState(false); const [createdId, setCreatedId] = useState<number | null>(null);
 const searchRef = useRef<HTMLInputElement>(null);
 useEffect(() => {
  if (!creating) searchRef.current?.closest('[data-slot="dialog-content"]')?.scrollTo(0, 0);
 }, [creating]);
 useEffect(() => {
  if (!open || creating) return;
  const focusSearch = (e: KeyboardEvent) => { if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); searchRef.current?.focus(); } };
  window.addEventListener('keydown', focusSearch); return () => window.removeEventListener('keydown', focusSearch);
 }, [open, creating]);
 const passwordValid = form.password.length >= 12 && new TextEncoder().encode(form.password).length <= 72;
 const loginValid = /^[A-Za-z0-9_.-]{3,32}$/.test(form.username);
 const filtered = (query.data ?? []).filter(user => user.username.toLowerCase().includes(search.toLowerCase()) && (filter === 'ALL' || user.role === filter));
 const resetForm = () => { setForm(emptyForm()); setError(''); setVisiblePassword(false); };
 const updateUser = (updated: ManagedUser) => { cache.setQueryData<ManagedUser[]>(['managed-users'], previous => (previous ?? []).map(user => user.id === updated.id ? updated : user)); void cache.invalidateQueries({ queryKey: ['managed-users'] }); };
 function generatePassword() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*?';
  const values = crypto.getRandomValues(new Uint32Array(20));
  setForm(current => ({ ...current, password: Array.from(values, value => alphabet[value % alphabet.length]).join('') })); setVisiblePassword(true);
 }
 async function submit(e: FormEvent) {
  e.preventDefault(); if (busy || !validNewUser(form)) return;
  setBusy(true); setError('');
  try {
   const created = await createUser(form);
   cache.setQueryData<ManagedUser[]>(['managed-users'], previous => [created, ...(previous ?? []).filter(user => user.id !== created.id)]);
   setCreatedId(created.id); setSearch(''); setFilter('ALL'); resetForm(); setCreating(false);
   toast.success(t('users.createdNamed', { username: created.username })); void cache.invalidateQueries({ queryKey: ['managed-users'] });
  } catch (e) { setError(e instanceof Error ? e.message : String(e)); }
  finally { setBusy(false); }
 }
 if (creating) return <div className="space-y-6 animate-in fade-in slide-in-from-right-2 duration-150 motion-reduce:animate-none">
  <button className="inline-flex items-center gap-2 rounded-lg pr-4 text-sm text-zinc-600 transition-colors hover:text-zinc-950 focus-visible:outline-2 focus-visible:outline-zinc-500 disabled:opacity-40" disabled={busy} onClick={() => { resetForm(); setCreating(false); }}><ArrowLeft size={17}/>{tr('title')}</button>
  <DialogHeader className="text-left"><DialogTitle className="text-2xl font-semibold tracking-tight">{tr('create')}</DialogTitle><DialogDescription className="text-zinc-500">{tr('createDescription')}</DialogDescription></DialogHeader>
  <form onSubmit={e => void submit(e)} className="space-y-6">
   <div className="grid items-start gap-7 md:grid-cols-[.85fr_1.15fr]">
    <div className="space-y-6">
     <div><label htmlFor="managed-username" className="mb-2 block text-sm font-medium">{tr('login')}</label><div className="relative"><input id="managed-username" autoFocus autoComplete="off" spellCheck={false} className={`${input} pr-10`} maxLength={32} disabled={busy} value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} aria-describedby="managed-login-hint" required/>{loginValid && <Check size={17} className="absolute right-3.5 top-3.5 text-emerald-600"/>}</div><p id="managed-login-hint" className="mt-2 text-xs leading-relaxed text-zinc-500">{tr('loginHint')}</p></div>
     <div><label htmlFor="managed-password" className="mb-2 block text-sm font-medium">{tr('password')}</label><div className="relative"><input id="managed-password" type={visiblePassword ? 'text' : 'password'} autoComplete="new-password" className={`${input} pr-12`} minLength={12} maxLength={72} disabled={busy} value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} aria-describedby="managed-password-hint" required/><button type="button" className="absolute right-1 top-1 flex h-9 w-9 items-center justify-center rounded-lg text-zinc-500 hover:bg-zinc-100 focus-visible:outline-2 focus-visible:outline-zinc-500" disabled={busy} aria-label={tr(visiblePassword ? 'hidePassword' : 'showPassword')} onClick={() => setVisiblePassword(!visiblePassword)}>{visiblePassword ? <EyeOff size={17}/> : <Eye size={17}/>}</button></div>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-2"><button type="button" className="rounded-lg bg-zinc-100 px-3 py-2 text-xs font-medium transition-colors hover:bg-zinc-200 focus-visible:outline-2 focus-visible:outline-zinc-500 disabled:opacity-40" disabled={busy} onClick={generatePassword}>{tr('generate')}</button>{form.password && <span className={`flex items-center gap-1 text-[11px] ${passwordValid ? 'text-emerald-700' : 'text-zinc-500'}`}>{passwordValid && <Check size={13}/>} {tr(passwordValid ? 'passwordValid' : 'passwordIncomplete')}</span>}</div><p id="managed-password-hint" className="mt-2 text-xs leading-relaxed text-zinc-500">{tr('passwordHint')}</p>
     </div>
    </div>
    <div><p className="mb-2 text-sm font-medium">{tr('role')}</p><RoleChoices name="new-user-role" value={form.role} onChange={role => setForm({ ...form, role })} disabled={busy}/></div>
   </div>
   <p className="flex items-start gap-3 rounded-xl bg-zinc-50 p-4 text-xs leading-relaxed text-zinc-500"><Info size={18} className="mt-0.5 shrink-0 text-zinc-800"/>{tr('createHint')}</p>
   {error && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
   <div className="flex flex-wrap justify-end gap-2 border-t border-zinc-100 pt-5"><button type="button" className={`${button} bg-zinc-100 text-zinc-800 hover:bg-zinc-200`} disabled={busy} onClick={() => { resetForm(); setCreating(false); }}>{tr('cancel')}</button><button className={primary} disabled={busy || !validNewUser(form)}>{busy && <RefreshCw size={16} className="animate-spin motion-reduce:animate-none"/>}{tr('create')}</button></div>
  </form>
 </div>;
 return <>
  <div className="flex flex-wrap items-center justify-between gap-4 pr-6"><DialogHeader className="text-left"><DialogTitle className="flex items-center gap-3 text-2xl font-semibold tracking-tight"><Users size={27} strokeWidth={1.8}/>{tr('title')}</DialogTitle><DialogDescription className="sm:pl-10 text-zinc-500">{tr('description')}</DialogDescription></DialogHeader><button className={primary} disabled={busy} onClick={() => { resetForm(); setCreating(true); }}><Plus size={16}/>{tr('add')}</button></div>
  <div className="flex items-center gap-2"><div className="relative min-w-0 flex-1"><Search size={17} className="pointer-events-none absolute left-3.5 top-3.5 text-zinc-500"/><input ref={searchRef} className={`${input} bg-zinc-50/80 pl-10 pr-10`} value={search} onChange={e => setSearch(e.target.value)} placeholder={tr('search')} aria-label={tr('search')}/>{search && <button className="absolute right-1 top-1 flex h-9 w-9 items-center justify-center rounded-lg text-zinc-400 hover:bg-zinc-100" aria-label={tr('clearSearch')} onClick={() => { setSearch(''); searchRef.current?.focus(); }}><X size={15}/></button>}</div>
   <Popover><PopoverTrigger asChild><button className={`${secondary} relative w-11 !px-0 ${filter !== 'ALL' ? 'border-zinc-800 bg-zinc-100' : ''}`} aria-label={tr('filterRoles')}><SlidersHorizontal size={17}/>{filter !== 'ALL' && <span className="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-zinc-800"/>}</button></PopoverTrigger><PopoverContent align="end" className={`${panel} w-64 p-4`}><label htmlFor="managed-role-filter" className="mb-2 block text-sm font-medium">{tr('filterRoles')}</label><select id="managed-role-filter" className={input} value={filter} onChange={e => setFilter(e.target.value)}><option value="ALL">{tr('allRoles')}</option>{roles.map(role => <option key={role} value={role}>{tr(`roles.${role}`)}</option>)}</select></PopoverContent></Popover>
   <button className={`${secondary} w-11 !px-0`} disabled={busy || query.isFetching} onClick={() => void query.refetch()} aria-label={tr('refresh')}><RefreshCw size={17} className={query.isFetching ? 'animate-spin motion-reduce:animate-none' : ''}/></button>
  </div>
  {query.isError && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{query.error.message}</p>}
  <div className="overflow-hidden rounded-2xl border border-zinc-100"><table className="w-full table-fixed text-left"><thead className="bg-zinc-50 text-[11px] font-normal text-zinc-500"><tr><th className="px-4 py-3 font-normal">{tr('userColumn')}</th><th className="hidden w-[44%] px-3 py-3 font-normal sm:table-cell">{tr('role')}</th><th className="w-20 px-3 py-3 text-right font-normal sm:w-24">{tr('actionsColumn')}</th></tr></thead><tbody>
   {query.isPending ? <tr><td colSpan={3} className="p-10 text-center text-sm text-zinc-400"><RefreshCw size={18} className="mx-auto mb-3 animate-spin motion-reduce:animate-none"/>{tr('loading')}</td></tr> : filtered.map(user => <UserRow key={`${user.id}:${user.role}`} user={user} self={user.username === username} fresh={createdId === user.id} onChanged={updateUser} onDeleted={id=>{cache.setQueryData<ManagedUser[]>(['managed-users'],old=>old?.filter(u=>u.id!==id));void cache.invalidateQueries({queryKey:['managed-users']});}} setMutationBusy={setBusy}/>)}
   {!query.isPending && !query.isError && filtered.length === 0 && <tr><td colSpan={3} className="p-10 text-center text-sm text-zinc-500">{tr('empty')}</td></tr>}
  </tbody></table></div>
  <div className="flex flex-wrap items-start justify-between gap-3 text-xs text-zinc-500"><span className="shrink-0">{t('users.shownCount', { count: filtered.length, total: query.data?.length ?? 0 })}</span></div>
 </>;
}
