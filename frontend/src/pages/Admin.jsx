import { useState, useEffect } from 'react'
import { getAllBuses, getAllRoutes } from '../api'
import StatusBadge from '../components/StatusBadge'
import OccupancyBar from '../components/OccupancyBar'

export default function Admin() {
  const [buses, setBuses] = useState([])
  const [routes, setRoutes] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [b, r] = await Promise.all([getAllBuses(), getAllRoutes()])
        setBuses(b || [])
        setRoutes(r || [])
      } catch (err) {
        console.error('Failed to load admin data:', err)
      }
      setLoading(false)
    }
    load()
    const interval = setInterval(load, 15000)
    return () => clearInterval(interval)
  }, [])

  const activeBuses = buses.filter(b => b.status === 'ACTIVE').length
  const breakdownBuses = buses.filter(b => b.status === 'BREAKDOWN')
  const totalStops = routes.reduce((sum, r) => sum + (r.stopCount || 0), 0)

  if (loading) {
    return <div className="text-center text-slate-400 py-12">Loading dashboard...</div>
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-6">Admin Dashboard</h1>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Active Buses" value={activeBuses} icon="🚌" color="text-green-400" />
        <StatCard label="Total Stops" value={totalStops} icon="📍" color="text-blue-400" />
        <StatCard label="Routes" value={routes.length} icon="🛤️" color="text-purple-400" />
        <StatCard label="Breakdowns" value={breakdownBuses.length} icon="⚠️" color="text-red-400" />
      </div>

      {/* Breakdown alerts */}
      {breakdownBuses.length > 0 && (
        <div className="mb-6 bg-red-500/10 border border-red-500/30 rounded-lg p-4">
          <h2 className="text-sm font-semibold text-red-400 mb-2">⚠️ Active Breakdowns</h2>
          {breakdownBuses.map(b => (
            <p key={b.id} className="text-sm text-red-300">
              Bus <span className="font-mono font-bold">{b.busNumber}</span> on Route {b.routeNumber}
            </p>
          ))}
        </div>
      )}

      {/* Bus table */}
      <div className="bg-[#1e293b] border border-[#334155] rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#334155] text-left">
                <th className="px-4 py-3 text-slate-400 font-medium">Bus #</th>
                <th className="px-4 py-3 text-slate-400 font-medium">Route</th>
                <th className="px-4 py-3 text-slate-400 font-medium">Status</th>
                <th className="px-4 py-3 text-slate-400 font-medium">Capacity</th>
              </tr>
            </thead>
            <tbody>
              {buses.map(bus => (
                <tr
                  key={bus.id}
                  className={`border-b border-[#334155] hover:bg-[#334155]/30 ${
                    bus.status === 'BREAKDOWN' ? 'bg-red-500/5' : ''
                  }`}
                >
                  <td className="px-4 py-3 text-orange-400 font-mono font-bold">{bus.busNumber}</td>
                  <td className="px-4 py-3 text-slate-300">
                    {bus.routeNumber}
                    {bus.routeName && <span className="text-slate-500 ml-1 text-xs">({bus.routeName})</span>}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={bus.status} /></td>
                  <td className="px-4 py-3 text-slate-400">{bus.capacity || '--'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value, icon, color }) {
  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-lg p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-2xl">{icon}</span>
      </div>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
      <p className="text-xs text-slate-500 mt-1">{label}</p>
    </div>
  )
}
