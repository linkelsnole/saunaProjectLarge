import { expect, it } from 'vitest';
import { decodeRegisterStates } from './RegisterStates';
it('decodes the reported 0040/0003 example', () => {
 expect(decodeRegisterStates(0x40, 3)).toEqual(['Вентилирование', 'D1 · Приточная заслонка открыта', 'D2 · Вытяжная заслонка открыта']);
});
it('keeps missing readings unknown and unknown bits visible', () => {
 expect(decodeRegisterStates(null, null).join(' ')).not.toContain('закрыта');
 expect(decodeRegisterStates(0x8000, 4).join(' ')).toContain('назначение неизвестно');
 expect(decodeRegisterStates(1, 0).join(' ')).toContain('D1 · Приточная заслонка закрыта');
});
