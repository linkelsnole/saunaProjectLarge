import { expect, it } from 'vitest';
import { radialValue } from './radialValue';
it.each([[7000,100],[21000,500]])('maps endpoints, midpoint and rounds to step for %s', (max,step) => {
 expect(radialValue(-1,1,max,step)).toBe(0);
 expect(radialValue(1,1,max,step)).toBe(max);
 expect(radialValue(0,-1,max,step)).toBe(max/2);
 expect(radialValue(1,-0.2,max,step)%step).toBe(0);
 expect(radialValue(-0.1,1,max,step)).toBe(0);
 expect(radialValue(0.1,1,max,step)).toBe(max);
});
