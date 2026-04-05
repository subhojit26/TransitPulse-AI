import { statusBadge } from '../utils'

export default function StatusBadge({ status }) {
  const badge = statusBadge(status)
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${badge.class}`}>
      {badge.text}
    </span>
  )
}
