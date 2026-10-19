type Props = { state: number | null; workState: number | null; stale: boolean };
// State meanings match ModbusConstants.SYSTEM_STATES; damper masks match DeviceService.
const stateBits = ['Выключено', 'Крах коммуникации Modbus', 'Отключение по сбою', 'Быстрый запуск', 'Начальный запуск', 'Калибровка', 'Вентилирование', 'Остановка и выбег', 'Сбой', 'Авария', 'Крах по замерзанию', 'Крах'];
const workBits: Record<number, string> = {6: 'Событие замерзания', 7: 'Событие сбоя', 8: 'Событие аварии', 14: 'Событие Modbus', 15: 'Событие краха'};
export function decodeRegisterStates(state: number | null, work: number | null): string[] {
 const rows: string[] = [];
 if (state == null) rows.push('Состояние установки неизвестно');
 else {
  stateBits.forEach((name, bit) => { if (state & (1 << bit)) rows.push(name); });
  if (state === 0) rows.push('Режим установки не определён');
  for (let bit = 12; bit < 16; bit++) if (state & (1 << bit)) rows.push(`State · бит ${bit} — назначение неизвестно`);
 }
 if (work == null) rows.push('Положение заслонок неизвестно');
 else {
  rows.push(`D1 · Приточная заслонка ${(work & 1) ? 'открыта' : 'закрыта'}`);
  rows.push(`D2 · Вытяжная заслонка ${(work & 2) ? 'открыта' : 'закрыта'}`);
  for (let bit = 2; bit < 16; bit++) if (work & (1 << bit)) rows.push(workBits[bit] ?? `WorkState · бит ${bit} — назначение неизвестно`);
 }
 return rows;
}
export function RegisterStates({ state, workState, stale }: Props) {
 return <div className="mt-4 rounded-xl bg-slate-50 p-3 text-xs text-slate-700">
  <p className="mb-2 font-semibold">{stale ? 'Последние известные состояния · связь не подтверждена' : 'Состояния по регистрам контроллера'}</p>
  <ul className="grid gap-2 sm:grid-cols-2">{decodeRegisterStates(state, workState).map(label => <li key={label}>{label}</li>)}</ul>
 </div>;
}
