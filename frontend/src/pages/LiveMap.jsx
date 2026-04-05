import { useState, useEffect, useRef, useMemo } from 'react'
import { MapContainer, TileLayer, Marker, Popup, CircleMarker, Tooltip, useMap } from 'react-leaflet'
import L from 'leaflet'
import { createStompClient } from '../stomp'
import { getAllRoutes, getStopsByRoute, getAllBuses } from '../api'

// City centers
const CITIES = {
  pune:   { label: 'Pune',    lat: 18.5204, lng: 73.8567, zoom: 13 },
  mumbai: { label: 'Mumbai',  lat: 19.0176, lng: 72.8420, zoom: 12 },
  nyc:    { label: 'NYC (Live GTFS-RT)', lat: 40.7580, lng: -73.9855, zoom: 13 },
  all:    { label: 'All Cities', lat: 22.0, lng: 73.5, zoom: 6 },
}

// Bus icons by crowd level
function makeBusIcon(color, isLive) {
  const border = isLive ? '2px solid #00ffff' : '2px solid #fff'
  return L.divIcon({
    className: '',
    html: `<div style="
      background:${color};width:32px;height:32px;border-radius:50%;
      display:flex;align-items:center;justify-content:center;
      border:${border};font-size:16px;box-shadow:0 2px 8px rgba(0,0,0,0.5);
      transition: transform 0.3s;
    ">🚌</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  })
}

const icons = {
  green: makeBusIcon('#22c55e', false),
  yellow: makeBusIcon('#eab308', false),
  orange: makeBusIcon('#f97316', false),
  red: makeBusIcon('#ef4444', false),
  gray: makeBusIcon('#6b7280', false),
  greenLive: makeBusIcon('#22c55e', true),
  yellowLive: makeBusIcon('#eab308', true),
  orangeLive: makeBusIcon('#f97316', true),
  redLive: makeBusIcon('#ef4444', true),
  grayLive: makeBusIcon('#6b7280', true),
}

function getIcon(crowdLabel, isNyc) {
  const suffix = isNyc ? 'Live' : ''
  if (!crowdLabel) return isNyc ? icons.grayLive : icons.gray
  if (crowdLabel.includes('🟢') || crowdLabel.toLowerCase().includes('comfortable')) return icons['green' + suffix] || icons.green
  if (crowdLabel.includes('🟡') || crowdLabel.toLowerCase().includes('moderate')) return icons['yellow' + suffix] || icons.yellow
  if (crowdLabel.includes('🟠') || crowdLabel.toLowerCase().includes('crowded')) return icons['orange' + suffix] || icons.orange
  if (crowdLabel.includes('🔴') || crowdLabel.toLowerCase().includes('very')) return icons['red' + suffix] || icons.red
  return isNyc ? icons.grayLive : icons.gray
}

// Animated marker — smoothly moves to new position
function AnimatedMarker({ bus }) {
  const markerRef = useRef(null)
  const lat = bus.latitude ?? bus.lat ?? 0
  const lng = bus.longitude ?? bus.lng ?? 0
  const isNyc = (bus.busNumber || '').startsWith('NYC-')
  const source = isNyc ? 'GTFS-RT Live' : 'Simulator'

  useEffect(() => {
    const marker = markerRef.current
    if (marker) {
      const target = L.latLng(lat, lng)
      const current = marker.getLatLng()
      const steps = 30
      const dLat = (target.lat - current.lat) / steps
      const dLng = (target.lng - current.lng) / steps
      let step = 0
      const interval = setInterval(() => {
        step++
        if (step >= steps) {
          marker.setLatLng(target)
          clearInterval(interval)
        } else {
          marker.setLatLng([
            current.lat + dLat * step,
            current.lng + dLng * step,
          ])
        }
      }, 2500 / steps)
      return () => clearInterval(interval)
    }
  }, [lat, lng])

  return (
    <Marker
      ref={markerRef}
      position={[lat, lng]}
      icon={getIcon(bus.crowdLabel, isNyc)}
    >
      <Popup>
        <div style={{ minWidth: 160 }}>
          <b style={{ fontSize: 15 }}>{bus.busNumber}</b>
          <span style={{ fontSize: 10, marginLeft: 4, padding: '1px 5px',
            borderRadius: 4,
            background: isNyc ? '#06b6d4' : '#f97316', color: '#fff' }}>
            {source}
          </span><br/>
          <span>Occupancy: {bus.occupancyPercent ?? '--'}%</span><br/>
          <span>{bus.crowdLabel || ''}</span><br/>
          <span>Speed: {bus.speed != null ? `${Math.round(bus.speed)} km/h` : '--'}</span><br/>
          <span>ETA: {bus.etaMinutes != null ? `${Math.round(bus.etaMinutes)} min` : '--'}</span><br/>
          <span style={{ fontSize: 11, opacity: 0.7 }}>Status: {bus.status || 'ACTIVE'}</span>
        </div>
      </Popup>
    </Marker>
  )
}

// Component to fly map to new center on city change
function FlyToCity({ center, zoom }) {
  const map = useMap()
  useEffect(() => {
    map.flyTo([center.lat, center.lng], zoom, { duration: 1.5 })
  }, [center.lat, center.lng, zoom])
  return null
}

export default function LiveMap() {
  const [buses, setBuses] = useState([])
  const [stops, setStops] = useState([])
  const [wsConnected, setWsConnected] = useState(false)
  const [city, setCity] = useState('pune')
  const clientRef = useRef(null)
  const pollRef = useRef(null)

  useEffect(() => {
    // Load stops
    async function loadStops() {
      try {
        const routes = await getAllRoutes()
        const allStops = []
        for (const r of routes) {
          const s = await getStopsByRoute(r.id)
          allStops.push(...(s || []).map(st => ({ ...st, routeNumber: r.routeNumber })))
        }
        setStops(allStops)
      } catch (err) {
        console.error('Failed to load stops:', err)
      }
    }
    loadStops()

    // Connect to live bus feed via WebSocket
    try {
      const client = createStompClient((c) => {
        setWsConnected(true)
        c.subscribe('/topic/buses/live', (msg) => {
          try {
            setBuses(JSON.parse(msg.body))
          } catch (e) {
            console.error('Failed to parse bus data:', e)
          }
        })
      })
      clientRef.current = client
    } catch (err) {
      console.error('WebSocket connect failed:', err)
    }

    // Polling fallback
    async function pollBuses() {
      try {
        const data = await getAllBuses()
        setBuses(prev => prev.length > 0 ? prev : (data || []).map(b => ({
          busId: b.id,
          busNumber: b.busNumber,
          latitude: b.currentLat || 0,
          longitude: b.currentLng || 0,
          occupancyPercent: b.occupancyPercent || 0,
          crowdLabel: '',
          status: b.status,
        })))
      } catch (e) { /* ignore */ }
    }
    pollBuses()
    pollRef.current = setInterval(pollBuses, 5000)

    return () => {
      if (clientRef.current) clientRef.current.deactivate()
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [])

  // Classify a bus by city
  function busCity(bus) {
    const bn = bus.busNumber || ''
    if (bn.startsWith('NYC-')) return 'nyc'
    if (bn.startsWith('MUM-')) return 'mumbai'
    return 'pune' // PNQ- or default
  }

  // Filter buses and stops by selected city
  const filteredBuses = city === 'all' ? buses : buses.filter(b => busCity(b) === city)
  const currentCity = CITIES[city]

  // Count per city
  const counts = { pune: 0, mumbai: 0, nyc: 0 }
  buses.forEach(b => { const c = busCity(b); if (counts[c] !== undefined) counts[c]++ })

  return (
    <div className="h-[calc(100vh-5rem)] -mx-4 -my-6 relative">
      {/* Connection indicator + city selector */}
      <div className="absolute top-3 left-3 z-[1000] flex flex-col gap-2">
        <div className="flex items-center gap-2 bg-[#1e293b]/90 px-3 py-1.5 rounded-full border border-[#334155]">
          <span className={`w-2 h-2 rounded-full ${wsConnected ? 'bg-green-400 animate-pulse' : 'bg-yellow-400'}`} />
          <span className="text-xs text-slate-300">
            {wsConnected ? 'Live' : 'Connecting...'} &middot; {filteredBuses.length} buses
          </span>
        </div>
        {/* City tabs */}
        <div className="flex gap-1 bg-[#1e293b]/90 p-1 rounded-lg border border-[#334155]">
          {Object.entries(CITIES).map(([key, c]) => (
            <button
              key={key}
              onClick={() => setCity(key)}
              className={`px-3 py-1.5 rounded text-xs font-medium transition-all ${
                city === key
                  ? 'bg-orange-500 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-[#334155]'
              }`}
            >
              {c.label}
              {key !== 'all' && counts[key] > 0 && (
                <span className="ml-1 text-[10px] opacity-75">({counts[key]})</span>
              )}
            </button>
          ))}
        </div>
      </div>

      <MapContainer
        center={[currentCity.lat, currentCity.lng]}
        zoom={currentCity.zoom}
        className="h-full w-full"
        style={{ background: '#0f172a' }}
      >
        <FlyToCity center={currentCity} zoom={currentCity.zoom} />
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Stop markers */}
        {stops.map(stop => (
          <CircleMarker
            key={`stop-${stop.id}`}
            center={[stop.latitude, stop.longitude]}
            radius={7}
            pathOptions={{ color: '#f97316', fillColor: '#f97316', fillOpacity: 0.7, weight: 2 }}
          >
            <Tooltip direction="top" offset={[0, -8]} permanent={false}>
              <span className="text-sm font-medium">{stop.stopName}</span>
              <br />
              <span className="text-xs text-gray-500">Route {stop.routeNumber}</span>
            </Tooltip>
          </CircleMarker>
        ))}

        {/* Animated bus markers */}
        {filteredBuses.map(bus => (
          <AnimatedMarker key={bus.busId} bus={bus} />
        ))}
      </MapContainer>

      {/* Legend */}
      <div className="absolute bottom-4 right-4 bg-[#1e293b]/95 border border-[#334155] rounded-lg p-3 z-[1000]">
        <p className="text-xs font-semibold text-white mb-2">Crowd Level</p>
        {[
          { c: 'bg-green-500', l: '🟢 Comfortable (0-30%)' },
          { c: 'bg-yellow-500', l: '🟡 Moderate (31-60%)' },
          { c: 'bg-orange-500', l: '🟠 Crowded (61-80%)' },
          { c: 'bg-red-500', l: '🔴 Very Crowded (81-100%)' },
        ].map(item => (
          <div key={item.l} className="flex items-center gap-2 mb-1">
            <span className={`w-3 h-3 rounded-full ${item.c}`} />
            <span className="text-xs text-slate-300">{item.l}</span>
          </div>
        ))}
        <div className="border-t border-[#334155] mt-2 pt-2">
          <p className="text-xs font-semibold text-white mb-1">Data Source</p>
          <div className="flex items-center gap-2 mb-1">
            <span className="w-3 h-3 rounded-full bg-gray-500 border border-white" style={{borderWidth:2}} />
            <span className="text-xs text-slate-300">Simulator (Pune/Mumbai)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-gray-500 border border-cyan-400" style={{borderWidth:2}} />
            <span className="text-xs text-cyan-300">GTFS-RT Live (NYC MTA)</span>
          </div>
        </div>
      </div>
    </div>
  )
}
