import { filterPercent, filterColor } from './filterScale';
import './filterHealth.css';

type Props = { coefficient?: number | null; name: string };
export function FilterGauge({ coefficient, name }: Props) {
  const percent = filterPercent(coefficient);
  const color = filterColor(percent);
  return <div className="filter-gauge" title={`${name} · Чистота фильтра: ${percent === null ? 'нет данных' : `${percent}%`}`}>
    <div className="filter-gauge-track" role="progressbar" aria-label={`${name}: чистота фильтра`}
      aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent ?? undefined} aria-valuetext={percent === null ? 'Нет данных' : `${percent}%`}>
      <div className="filter-gauge-fill" style={{ height: `${percent ?? 0}%`, background: color }} />
    </div>
    <span style={{ color }}>{percent === null ? '—' : `${percent}%`}</span>
  </div>;
}
export function FilterHealthSummary({ supply, exhaust }: { supply?: number | null; exhaust?: number | null }) {
  return <section className="filter-summary" aria-label="Чистота фильтров">
    <header><h3>Чистота фильтров</h3><span>100% — чистый</span></header>
    <div className="filter-summary-grid">{[{ name: 'PF1', label: 'Приточный фильтр', k: supply }, { name: 'PF2', label: 'Вытяжной фильтр', k: exhaust }].map(({ name, label, k }) => {
      const p = filterPercent(k);
      return <div className="filter-summary-item" key={name}>
        <FilterGauge coefficient={k} name={name} />
        <div><div className="filter-summary-name">{name}<span>{label}</span></div>
        <div className="filter-summary-coefficient">Коэффициент <strong>{p === null ? '—' : k!.toFixed(2).replace('.', ',')}</strong></div></div>
      </div>;
    })}</div>
    <p>Коэффициент 0,9 → 100% · 3,5 → 0%</p>
  </section>;
}
