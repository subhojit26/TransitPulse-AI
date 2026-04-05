export function crowdColor(label) {
  if (!label) return 'bg-gray-500'
  if (label.includes('🟢') || label.toLowerCase().includes('comfortable')) return 'bg-green-500'
  if (label.includes('🟡') || label.toLowerCase().includes('moderate')) return 'bg-yellow-500'
  if (label.includes('🟠') || label.toLowerCase().includes('crowded')) return 'bg-orange-500'
  if (label.includes('🔴') || label.toLowerCase().includes('very')) return 'bg-red-500'
  return 'bg-gray-500'
}

export function crowdTextColor(label) {
  if (!label) return 'text-gray-400'
  if (label.includes('🟢') || label.toLowerCase().includes('comfortable')) return 'text-green-400'
  if (label.includes('🟡') || label.toLowerCase().includes('moderate')) return 'text-yellow-400'
  if (label.includes('🟠') || label.toLowerCase().includes('crowded')) return 'text-orange-400'
  if (label.includes('🔴') || label.toLowerCase().includes('very')) return 'text-red-400'
  return 'text-gray-400'
}

export function statusBadge(status) {
  switch (status?.toUpperCase()) {
    case 'ACTIVE': return { text: 'ON TIME', class: 'bg-green-500/20 text-green-400' }
    case 'DELAYED': return { text: 'DELAYED', class: 'bg-yellow-500/20 text-yellow-400' }
    case 'BREAKDOWN': return { text: 'BREAKDOWN', class: 'bg-red-500/20 text-red-400' }
    case 'INACTIVE': return { text: 'INACTIVE', class: 'bg-gray-500/20 text-gray-400' }
    default: return { text: status || 'UNKNOWN', class: 'bg-gray-500/20 text-gray-400' }
  }
}
