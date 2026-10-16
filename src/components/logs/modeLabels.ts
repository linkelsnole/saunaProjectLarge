// Only named mode fields are decoded. Numeric setpoints and unknown codes stay intact.
const volume = ['Отключен', 'ЭКО', 'Норм', 'Интенсив', 'Ручной'];
const temperature = ['Отключен', 'ЭКО', 'АВТО', 'Ручной'];
export function formatModeLog(text: string): string {
  return text.split('; ').map(line => {
    const match = /^(Режим (?:объёма|вентиляции|температуры зоны [12])): (\d+)(?: → (\d+))?$/.exec(line);
    if (!match) return line;
    const names = match[1].startsWith('Режим температуры') ? temperature : volume;
    const label = (code: string) => names[Number(code)] ?? `Неизвестный режим (${code})`;
    return `${match[1]}: ${label(match[2])}${match[3] == null ? '' : ` → ${label(match[3])}`}`;
  }).join('; ');
}
