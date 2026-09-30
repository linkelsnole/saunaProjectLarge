import { fetchWithAuth } from '@/api/authApi';
export interface PhysicalSlot { slotId: number; baseAddress: number; values: (number | null)[] }
export interface PhysicalSnapshot { slots: PhysicalSlot[]; reportedSlots: number | null; readAt: number }
export interface RegisterChange { offset: number; expected: number; value: number }
export const hex = (v: number) => `0x${v.toString(16).toUpperCase().padStart(4, '0')}`;
export const offsets = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16, 17];
export const names: Record<number, string> = { 0:'Mode',1:'State.Selected',2:'DaysOfWeek',3:'Date1',4:'Date2',5:'Time1',6:'Time2',7:'Delay',8:'Group',9:'PMode',10:'Pdst',12:'TMode1',13:'TControlMode1',14:'TDst1',15:'TMode2',16:'TControlMode2',17:'TDst2' };
export const enumKeys: Record<number, string> = { 0:'types',9:'ventilation',12:'temperature',15:'temperature',13:'control',16:'control' };
export function displayValue(values: (number|null)[], offset: number): string {
  const value = values[offset];
  if (value == null) return '';
  if (offset === 1) return String(value & 1);
  if (offset === 3 || offset === 4 || offset === 5 || offset === 6)
    return `${String(value >> 8).padStart(2,'0')}${offset < 5 ? '-' : ':'}${String(value & 255).padStart(2,'0')}`;
  if (offset === 14 || offset === 17) return String((value > 32767 ? value - 65536 : value) / 10);
  if (offset === 10) {
    if (values[11] == null) return '';
    const data = new DataView(new ArrayBuffer(4));
    data.setUint16(0,value,true); data.setUint16(2,values[11],true);
    return String(data.getFloat32(0,true));
  }
  return String(value);
}
export const slotDraft = (slot: PhysicalSlot) => Object.fromEntries(offsets.map(o => [o, displayValue(slot.values,o)])) as Record<number,string>;
export function changesFor(slot: PhysicalSlot, draft: Record<number,string>): RegisterChange[] {
  const changes: RegisterChange[] = [];
  for (const offset of offsets) {
    const text = draft[offset];
    if (text === displayValue(slot.values, offset)) continue;
    if (slot.values[offset] == null || (offset === 10 && slot.values[11] == null)) throw new Error('missing');
    if (!text.trim()) throw new Error(names[offset]);
    let words: number[];
    const value = Number(text);
    if (offset === 3 || offset === 4 || offset === 5 || offset === 6) {
      const match = text.match(offset < 5 ? /^(\d{2})-(\d{2})$/ : /^(\d{2}):(\d{2})$/);
      if (!match) throw new Error(names[offset]);
      const a=Number(match[1]), b=Number(match[2]);
      if (offset < 5) {
        const date=new Date(Date.UTC(2000,a-1,b));
        if (a<1 || a>12 || b<1 || date.getUTCMonth()!==a-1) throw new Error(names[offset]);
      } else if (a>23 || b>59) throw new Error(names[offset]);
      words=[(a<<8)|b];
    } else if (offset === 10) {
      if (!Number.isFinite(value) || value<0 || !Number.isFinite(Math.fround(value))) throw new Error(names[offset]);
      const data=new DataView(new ArrayBuffer(4)); data.setFloat32(0,value,true);
      words=[data.getUint16(0,true),data.getUint16(2,true)];
    } else if (offset===14 || offset===17) {
      if (!Number.isFinite(value) || value < -3276.8 || value > 3276.7 || Math.abs(value*10-Math.round(value*10))>1e-7) throw new Error(names[offset]);
      words=[Math.round(value*10)&65535];
    } else {
      const max=offset===0?5:offset===1?1:offset===2?127:offset===9?4:offset===12||offset===15?3:offset===13||offset===16?2:65535;
      if (!Number.isInteger(value) || value<0 || value>max) throw new Error(names[offset]);
      words=[value];
    }
    words.forEach((word,index) => changes.push({offset:offset+index,expected:slot.values[offset+index]!,value:word}));
  }
  return changes;
}
async function request<T>(path = '', options?: RequestInit): Promise<T> {
  const response=await fetchWithAuth(`/api/v1/devices/physical-schedules${path}`,options);
  if (!response.ok) {
    const body=await response.json().catch(()=>null);
    throw new Error(body?.message || `HTTP ${response.status}`);
  }
  return response.status===204 ? undefined as T : response.json();
}
export const readPhysicalSchedules=()=>request<PhysicalSnapshot>();
export const savePhysicalSlot=(id:number,changes:RegisterChange[])=>request<{slot:PhysicalSlot;verified:boolean}>(`/${id}`,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({changes,apply:true})});
export const applyPhysicalSchedules=()=>request<void>('/apply',{method:'POST'});
