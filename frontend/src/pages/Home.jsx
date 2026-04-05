import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getNearbyStops, getIncomingBuses } from '../api'
import EtaBadge from '../components/EtaBadge'
import OccupancyBar from '../components/OccupancyBar'

export default function Home() {
  const [query, setQuery] = useState('')
  const [stops, setStops] = useState([])
  const [busMap, setBusMap] = useState({})
  const [loading, setLoading] = useState(false)
  const [geoLoading, setGeoLoading] = useState(false)

  // Nagpur center as default
  const defaultLat = 21.1458
  const defaultLng = 79.0882

  useEffect(() => {
    loadNearby(defaultLat, defaultLng, 5000)
  }, [])

  async function loadNearby(lat, lng, radius) {
    setLoading(true)
    try {
      const data = await getNearbyStops(lat, lng, radius)
      setStops(data || [])
      // Load incoming buses for top stops
      const top = (data || []).slice(0, 6)
      const map = {}
      await Promise.all(
        top.map(async (s) => {
          try {
            map[s.stopId] = await getIncomingBuses(s.stopId)
          } catch { map[s.stopId] = [] }
        })
      )
      setBusMap(map)
    } catch (err) {
      console.error('Failed to load stops:', err)
    }
    setLoading(false)
  }

  function useMyLocation() {
    if (!navigator.geolocation) return
    setGeoLoading(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        loadNearby(pos.coords.latitude, pos.coords.longitude, 1000)
        setGeoLoading(false)
      },
      () => {
        loadNearby(defaultLat, defaultLng, 5000)
        setGeoLoading(false)
      }
    )
  }

  const filtered = query
    ? stops.filter(s => s.stopName.toLowerCase().includes(query.toLowerCase()))
    : stops

  return (
    <div>
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">
          <span className="text-orange-400">TransitPulse</span> AI
        </h1>
        <p className="text-slate-400">Real-time bus tracking for Nagpur</p>
      </div>

      {/* Search */}
      <div className="max-w-xl mx-auto mb-8 flex gap-2">
        <div className="flex-1 relative">
          <input
            type="text"
            placeholder="Find your stop..."
            value={query}
            onChange={e => setQuery(e.target.value)}
            className="w-full px-4 py-3 bg-[#1e293b] border border-[#334155] rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-orange-500 transition"
          />
          {query && (
            <button onClick={() => setQuery('')} className="absolute right-3 top-3 text-slate-500 hover:text-white">
              ✕
            </button>
          )}
        </div>
        <button
          onClick={useMyLocation}
          disabled={geoLoading}
          className="px-4 py-3 bg-orange-500 hover:bg-orange-600 text-white rounded-lg font-medium transition whitespace-nowrap disabled:opacity-50"
        >
          {geoLoading ? '...' : '📍 Near me'}
        </button>
      </div>

      {loading ? (
        <div className="text-center text-slate-400 py-12">Loading stops...</div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.slice(0, 6).map(stop => (
            <Link
              key={stop.stopId}
              to={`/stop/${stop.stopId}`}
              className="block bg-[#1e293b] border border-[#334155] rounded-lg p-4 hover:border-orange-500/50 transition group"
            >
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold text-white group-hover:text-orange-400 transition">
                    {stop.stopName}
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Route {stop.routeNumber} &middot; {stop.routeName}
                  </p>
                </div>
                {stop.distanceMeters && (
                  <span className="text-xs text-slate-500 bg-[#334155] px-2 py-0.5 rounded">
                    {stop.distanceMeters < 1000
                      ? `${Math.round(stop.distanceMeters)}m`
                      : `${(stop.distanceMeters / 1000).toFixed(1)}km`}
                  </span>
                )}
              </div>
              {/* Incoming buses */}
              {(busMap[stop.stopId] || []).slice(0, 2).map((bus, i) => (
                <div key={i} className="flex items-center justify-between py-1.5 border-t border-[#334155]">
                  <div className="flex items-center gap-2">
                    <span className="text-orange-400 font-mono text-sm">{bus.busNumber}</span>
                    <OccupancyBar percent={bus.occupancyPercent} label={bus.crowdLabel} />
                  </div>
                  <EtaBadge eta={parseEta(bus.eta)} crowdLabel={bus.crowdLabel} />
                </div>
              ))}
              {(!busMap[stop.stopId] || busMap[stop.stopId].length === 0) && (
                <p className="text-xs text-slate-500 border-t border-[#334155] pt-2">No buses approaching</p>
              )}
            </Link>
          ))}
        </div>
      )}

      {!loading && filtered.length === 0 && (
        <p className="text-center text-slate-500 py-12">No stops found. Try a different search.</p>
      )}
    </div>
  )
}

function parseEta(eta) {
  if (eta == null) return null
  if (typeof eta === 'number') return eta
  const m = String(eta).match(/(\d+)/)
  return m ? parseInt(m[1]) : null
}
