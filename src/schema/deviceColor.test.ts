import { expect, it } from 'vitest';
import { deviceColor } from './deviceColor';
it('colors only a selected healthy device green', () => {
 const states = [{id:'BF11',alarmMode:0}];
 expect(deviceColor(['M1_1'],null,states,true)).toBeUndefined();
 expect(deviceColor(['M1_1'],'M1_1',states,true)).toBe('#059669');
});
it('fault overrides selection and stays red without selection', () => {
 for (const selected of [null,'M1_1']) expect(deviceColor(['M1_1'],selected,[{id:'BF11',alarmMode:2}],true)).toBe('#dc2626');
});
it('does not imply healthy when data is unavailable or unknown', () => {
 expect(deviceColor(['M1_1'],'M1_1',[{id:'BF11',alarmMode:0}],false)).toBeUndefined();
 expect(deviceColor(['M1_1'],'M1_1',[],true)).toBeUndefined();
});
it('shared pressure gauge prioritizes fault over a healthy selection', () => {
 expect(deviceColor(['PR1','PR2'],'PR1',[{id:'BP10',alarmMode:0},{id:'BP5',alarmMode:3}],true)).toBe('#dc2626');
});
