import { crowdTextColor } from '../utils'

export default function EtaBadge({ eta, crowdLabel }) {
  if (eta == null) return <span className="text-gray-500 text-sm">--</span>
  const color = eta <= 5
    ? 'bg-green-500/20 text-green-400'
    : eta <= 15
      ? 'bg-yellow-500/20 text-yellow-400'
      : 'bg-red-500/20 text-red-400'

  return (
    <span className={`px-2 py-0.5 rounded text-xs font-bold ${color}`}>
      {Math.round(eta)} min
    </span>
  )
}
