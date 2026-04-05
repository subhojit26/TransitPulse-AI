import { useState, useEffect } from 'react'
import { getAllBuses, updateOccupancy } from '../api'

export default function Conductor() {
  const [buses, setBuses] = useState([])
  const [selectedBus, setSelectedBus] = useState('')
  const [occupancy, setOccupancy] = useState(50)
  const [submitting, setSubmitting] = useState(false)
  const [history, setHistory] = useState([])

  useEffect(() => {
    getAllBuses().then(setBuses).catch(() => {})
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    if (!selectedBus) return
    setSubmitting(true)
    try {
      const result = await updateOccupancy({
        busId: parseInt(selectedBus),
        occupancyPercent: occupancy,
        conductorId: 'conductor-web',
      })
      setHistory(prev => [
        {
          busNumber: result.busNumber || buses.find(b => b.id == selectedBus)?.busNumber,
          occupancyPercent: occupancy,
          crowdLabel: result.crowdLabel,
          time: new Date().toLocaleTimeString(),
        },
        ...prev,
      ].slice(0, 5))
    } catch (err) {
      console.error('Update failed:', err)
    }
    setSubmitting(false)
  }

  function occupancyColor() {
    if (occupancy <= 30) return 'text-green-400'
    if (occupancy <= 60) return 'text-yellow-400'
    if (occupancy <= 80) return 'text-orange-400'
    return 'text-red-400'
  }

  return (
    <div className="max-w-md mx-auto">
      <h1 className="text-2xl font-bold text-white mb-1">Conductor Panel</h1>
      <p className="text-xs text-slate-500 mb-6">Auth to be added in a future update</p>

      <form onSubmit={handleSubmit} className="bg-[#1e293b] border border-[#334155] rounded-lg p-6 mb-6">
        {/* Bus select */}
        <label className="block mb-4">
          <span className="text-sm text-slate-400 block mb-1">Select Bus</span>
          <select
            value={selectedBus}
            onChange={e => setSelectedBus(e.target.value)}
            className="w-full px-3 py-3 bg-[#0f172a] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-orange-500"
          >
            <option value="">-- Select bus --</option>
            {buses.map(b => (
              <option key={b.id} value={b.id}>
                {b.busNumber} — {b.routeNumber} ({b.routeName})
              </option>
            ))}
          </select>
        </label>

        {/* Occupancy slider */}
        <label className="block mb-6">
          <span className="text-sm text-slate-400 block mb-1">Occupancy</span>
          <div className="text-center mb-2">
            <span className={`text-4xl font-bold ${occupancyColor()}`}>{occupancy}%</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            value={occupancy}
            onChange={e => setOccupancy(parseInt(e.target.value))}
            className="w-full h-2 bg-[#334155] rounded-lg appearance-none cursor-pointer accent-orange-500"
          />
          <div className="flex justify-between text-xs text-slate-600 mt-1">
            <span>0%</span>
            <span>50%</span>
            <span>100%</span>
          </div>
        </label>

        <button
          type="submit"
          disabled={!selectedBus || submitting}
          className="w-full py-4 bg-orange-500 hover:bg-orange-600 disabled:bg-slate-600 text-white text-lg font-bold rounded-lg transition"
        >
          {submitting ? 'Updating...' : '📡 Update Occupancy'}
        </button>
      </form>

      {/* History */}
      {history.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-slate-400 mb-2">Recent Updates</h2>
          <div className="space-y-2">
            {history.map((h, i) => (
              <div key={i} className="bg-[#1e293b] border border-[#334155] rounded-lg px-4 py-2 flex items-center justify-between">
                <div>
                  <span className="text-orange-400 font-mono text-sm">{h.busNumber}</span>
                  <span className="text-slate-400 text-sm ml-2">{h.occupancyPercent}%</span>
                  {h.crowdLabel && <span className="text-xs text-slate-500 ml-2">{h.crowdLabel}</span>}
                </div>
                <span className="text-xs text-slate-600">{h.time}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
