import { crowdColor } from '../utils'

export default function OccupancyBar({ percent, label }) {
  const pct = percent ?? 0
  return (
    <div className="w-full">
      <div className="flex justify-between text-xs mb-1">
        <span className="text-slate-400">{pct}% occupied</span>
        {label && <span className="text-slate-300">{label}</span>}
      </div>
      <div className="w-full h-2 bg-[#334155] rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-500 ${crowdColor(label)}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}
