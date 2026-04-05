import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getAllStops, getIncomingBuses } from '../api'
import EtaBadge from '../components/EtaBadge'
import OccupancyBar from '../components/OccupancyBar'

export default function Home() {
  const [query, setQuery] = useState('')
  const [stops, setStops] = useState([])
  const [busMap, setBusMap] = useState({})
  const [loading, setLoading] = useState(true)
  const [geoLoading, setGeoLoading] = useState(false)
  const [dropOpen, setDropOpen] = useState(false)
  const [userLat, setUserLat] = useState(18.5204)
  const [userLng, setUserLng] = useState(73.8567)
  const dropRef = useRef(null)
  const navigate = useNavigate()

  // Close dropdown on outside click
  useEffect(() => {
    function handleClick(e) {
      if (dropRef.current && !dropRef.current.contains(e.target)) {
        setDropOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  // Load all stops sorted by distance
  useEffect(() => {
    loadStops(userLat, userLng)
  }, [userLat, userLng])

  async function loadStops(lat, lng) {
    setLoading(true)
    try {
      const data = await getAllStops(lat, lng)
      setStops(data || [])
      // Load incoming buses for top 6 stops
      const top = (data || []).slice(0, 6)
      const map = {}
      await Promise.all(
        top.map(async (s) => {
          try { map[s.stopId] = await getIncomingBuses(s.stopId) }
          catch { map[s.stopId] = [] }
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
        setUserLat(pos.coords.latitude)
        setUserLng(pos.coords.longitude)
        setGeoLoading(false)
      },
      () => {
        setGeoLoading(false)
      }
    )
  }

  function formatDist(m) {
    if (m == null) return ''
    return m < 1000 ? `${Math.round(m)}m` : `${(m / 1000).toFixed(1)}km`
  }

  // Filter stops for the dropdown
  const filtered = query
    ? stops.filter(s =>
        s.stopName.toLowerCase().includes(query.toLowerCase()) ||
        s.routeNumber?.toLowerCase().includes(query.toLowerCase()) ||
        s.routeName?.toLowerCase().includes(query.toLowerCase())
      )
    : stops

  // Top 6 stops for the cards (always closest)
  const topStops = stops.slice(0, 6)

  return (
    <div>
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">
          <span className="text-orange-400">TransitPulse</span> AI
        </h1>
        <p className="text-slate-400">Real-time bus tracking — Pune, Mumbai &amp; NYC (Live GTFS-RT)</p>
      </div>

      {/* Search + Dropdown */}
      <div className="max-w-xl mx-auto mb-8 flex gap-2">
        <div className="flex-1 relative" ref={dropRef}>
          <input
            type="text"
            placeholder="Search stops by name or route..."
            value={query}
            onChange={e => { setQuery(e.target.value); setDropOpen(true) }}
            onFocus={() => setDropOpen(true)}
            className="w-full px-4 py-3 bg-[#1e293b] border border-[#334155] rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-orange-500 transition"
          />
          {query && (
            <button onClick={() => { setQuery(''); setDropOpen(false) }} className="absolute right-3 top-3 text-slate-500 hover:text-white">
              ✕
            </button>
          )}

          {/* Stops Dropdown */}
          {dropOpen && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-[#1e293b] border border-[#334155] rounded-lg shadow-xl z-50 max-h-80 overflow-y-auto">
              {filtered.length === 0 ? (
                <div className="px-4 py-3 text-slate-500 text-sm">No stops found</div>
              ) : (
                filtered.map(stop => (
                  <button
                    key={stop.stopId}
                    onClick={() => {
                      setDropOpen(false)
                      setQuery('')
                      navigate(`/stop/${stop.stopId}`)
                    }}
                    className="w-full text-left px-4 py-3 hover:bg-[#334155] transition flex items-center justify-between gap-3 border-b border-[#334155]/50 last:border-0"
                  >
                    <div className="min-w-0">
                      <div className="text-white text-sm font-medium truncate">{stop.stopName}</div>
                      <div className="text-xs text-slate-500 truncate">
                        Route {stop.routeNumber} &middot; {stop.routeName}
                      </div>
                    </div>
                    <span className="text-xs text-orange-400 bg-orange-400/10 px-2 py-0.5 rounded whitespace-nowrap">
                      {formatDist(stop.distanceMeters)}
                    </span>
                  </button>
                ))
              )}
            </div>
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

      {/* Nearest stops cards */}
      <h2 className="text-lg font-semibold text-white mb-4">
        Nearest Stops
        <span className="text-sm text-slate-500 font-normal ml-2">({stops.length} total)</span>
      </h2>

      {loading ? (
        <div className="text-center text-slate-400 py-12">Loading stops...</div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {topStops.map(stop => (
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
                {stop.distanceMeters != null && (
                  <span className="text-xs text-orange-400 bg-orange-400/10 px-2 py-0.5 rounded">
                    {formatDist(stop.distanceMeters)}
                  </span>
                )}
              </div>
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

      {!loading && stops.length === 0 && (
        <p className="text-center text-slate-500 py-12">No stops found.</p>
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
