type Props = { state: number | null; workState: number | null; state2?: number | null; workState2?: number | null; stale: boolean };
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
const workLabels: Record<number, string> = {0:'D1 · Приточная заслонка',1:'D2 · Вытяжная заслонка',2:'Отключение нагрева/охлаждения · зона 1',3:'Отключение нагрева/охлаждения · зона 2',4:'Расчёт по массе воздуха',5:'Снижение оборотов при замерзании',6:'Событие замерзания',7:'Событие сбоя',8:'Событие аварии',11:'Замерзание',12:'Недостаточный нагрев/охлаждение · зона 1',13:'Недостаточный нагрев/охлаждение · зона 2',14:'Событие отказа Modbus',15:'Событие краха'};
// Air2Common.xls (2026-10-21), Air2 rows 171–221. Unmapped bits are intentionally not guessed.
const state2Labels: Record<number,string> = {0:'Сбой',1:'Авария',2:'Крах: заморозка',3:'Режим тестирования',4:'Сброс оборудования'};
const work2Labels: Record<number,string> = {0:'Вытяжка работает',1:'Вытяжка приостановлена',2:'D3 · Шторка вытяжки',3:'D4 · Шторка вытяжки',4:'Ручное включение вытяжки',5:'Рекуперация отключена',6:'Насос рекуператора',7:'Насос нагрева · зона 1',8:'Насос нагрева · зона 2',9:'Условия аварий',10:'Фильтр ПВ засорён',11:'Фильтр ОВ засорён'};
function Register({name,address,value,labels,doors=[]}: {name:string;address:string;value:number|null;labels:Record<number,string>;doors?:number[]}) {
 const unknown = value == null ? [] : Array.from({length:16},(_,bit)=>bit).filter(bit=>(value & (1<<bit)) && !(bit in labels));
 return <section className="min-w-0 rounded-xl border border-slate-200 p-3">
  <h4 className="mb-2 flex flex-wrap items-center justify-between gap-2 text-xs font-semibold text-slate-800">{name}<span className="font-mono text-[10px] font-normal text-slate-400">{address}{value != null && ` · 0x${value.toString(16).toUpperCase().padStart(4,'0')}`}</span></h4>
  {value == null ? <p className="text-xs text-slate-500">Нет данных</p> : <ul className="space-y-1.5">{Object.entries(labels).map(([bit,label])=>{const active=!!(value&(1<<Number(bit)));return <li key={bit} className="flex items-start gap-2 text-xs"><span className="w-4 shrink-0 font-mono text-[10px] text-slate-400" title={`Бит ${bit}`}>{bit}</span><span className="min-w-0 flex-1 text-slate-600">{label}</span><span className={`shrink-0 font-medium ${active?'text-slate-900':'text-slate-400'}`}>{doors.includes(Number(bit)) ? active?'Открыта':'Закрыта' : active?'Да':'Нет'}</span></li>})}</ul>}
  {unknown.length>0 && <p className="mt-2 text-xs text-amber-700">Неизвестные активные биты: {unknown.join(', ')}</p>}
 </section>;
}
export function RegisterStates({state,state2=null,workState,workState2=null,stale}:Props) {
 return <div className="mt-3">
  {stale && <p className="mb-2 text-xs text-amber-700">Последние известные данные · связь не подтверждена</p>}
  <div className="grid gap-3 @[600px]:grid-cols-2">
   <Register name="State" address="0x0301" value={state} labels={Object.fromEntries(stateBits.map((name,bit)=>[bit,name]))}/>
   <Register name="WorkState" address="0x0305" value={workState} labels={workLabels} doors={[0,1]}/>
   <Register name="State2" address="0x0302" value={state2} labels={state2Labels}/>
   <Register name="WorkState2" address="0x0306" value={workState2} labels={work2Labels} doors={[2,3]}/>
  </div>
 </div>;
}
