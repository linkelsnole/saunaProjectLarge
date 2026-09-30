import { describe, expect, it } from 'vitest';
import { changesFor, displayValue, slotDraft, type PhysicalSlot } from './physicalSchedule';
const slot=():PhysicalSlot=>({slotId:7,baseAddress:0x20c0,values:Array(18).fill(0)});
describe('physical schedule register encoding',()=>{
 it('keeps untouched unknown and unavailable registers',()=>{const s=slot();s.values[0]=99;s.values[12]=null;expect(changesFor(s,slotDraft(s))).toEqual([]);});
 it('writes only changed group',()=>{const s=slot();expect(changesFor(s,{...slotDraft(s),8:'42'})).toEqual([{offset:8,expected:0,value:42}]);});
 it('uses Selected only while preserving state as expected value',()=>{const s=slot();s.values[1]=2;expect(changesFor(s,{...slotDraft(s),1:'1'})).toEqual([{offset:1,expected:2,value:1}]);});
 it('packs a float into two low-word-first registers',()=>{const s=slot();const p=changesFor(s,{...slotDraft(s),10:'1500'});expect(p.map(c=>c.offset)).toEqual([10,11]);p.forEach(c=>s.values[c.offset]=c.value);expect(displayValue(s.values,10)).toBe('1500');expect(p[0].value).toBe(32768);expect(p[1].value).toBe(17595);});
 it('encodes negative temperature as signed tenths',()=>{const s=slot();expect(changesFor(s,{...slotDraft(s),14:'-5'})).toEqual([{offset:14,expected:0,value:65486}]);s.values[14]=65486;expect(displayValue(s.values,14)).toBe('-5');});
 it('packs month/day without a year and hours/minutes',()=>{const s=slot();expect(changesFor(s,{...slotDraft(s),3:'09-15',5:'21:38'}).map(c=>c.value)).toEqual([0x090f,0x1526]);});
 it('rejects impossible dates times values and float overflow',()=>{const s=slot();for(const [o,v] of [[3,'02-30'],[5,'24:01'],[6,'12:60'],[12,'4'],[10,'1e100'],[14,'0.15'],[8,'']] as const) expect(()=>changesFor(s,{...slotDraft(s),[o]:v})).toThrow();});
 it('keeps manual and ECO independent for both zones',()=>{const s=slot();expect(changesFor(s,{...slotDraft(s),12:'3',15:'1'}).map(c=>c.value)).toEqual([3,1]);});
});
