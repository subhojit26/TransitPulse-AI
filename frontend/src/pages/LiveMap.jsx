import { useState, useEffect, useRef } from 'react'
import { MapContainer, TileLayer, Marker, Popup, CircleMarker, Tooltip } from 'react-leaflet'
import L from 'leaflet'
import { createStompClient } from '../stomp'
import { getAllRoutes, getStopsByRoute } from '../api'

// Bus icons by crowd level
function makeBusIcon(color) {
  return L.divIcon({
    className: '',
    html: `<div style="
      background:${color};width:28px;height:28px;border-radius:50%;
      display:flex;align-items:center;justify-content:center;
      border:2px solid #fff;font-size:14px;box-shadow:0 2px 6px rgba(0,0,0,0.4);
    ">🚌</div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })
}

const icons = {
  green: makeBusIcon('#22c55e'),
  yellow: makeBusIcon('#eab308'),
  orange: makeBusIcon('#f97316'),
  red: makeBusIcon('#ef4444'),
  gray: makeBusIcon('#6b7280'),
}

function busIcon(crowdLabel) {
  if (!crowdLabel) return icons.gray
  if (crowdLabel.includes('🟢') || crowdLabel.toLowerCase().includes('comfortable')) return icons.green
  if (crowdLabel.includes('🟡') || crowdLabel.toLowerCase().includes('moderate')) return icons.yellow
  if (crowdLabel.includes('🟠') || crowdLabel.toLowerCase().includes('crowded')) return icons.orange
  if (crowdLabel.includes('🔴') || crowdLabel.toLowerCase().includes('very')) return icons.red
  return icons.gray
}

export default function LiveMap() {
  const [buses, setBuses] = useState([])
  const [stops, setStops] = useState([])
  const clientRef = useRef(null)

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

    // Connect to live bus feed
    const client = createStompClient((c) => {
      c.subscribe('/topic/buses/live', (msg) => {
        setBuses(JSON.parse(msg.body))
      })
    })
    clientRef.current = client

    return () => {
      if (clientRef.current) clientRef.current.deactivate()
    }
  }, [])

  return (
    <div className="h-[calc(100vh-5rem)] -mx-4 -my-6 relative">
      <MapContainer
        center={[21.1458, 79.0882]}
        zoom={13}
        className="h-full w-full"
        style={{ background: '#0f172a' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Stop markers */}
        {stops.map(stop => (
          <CircleMarker
            key={`stop-${stop.id}`}
            center={[stop.latitude, stop.longitude]}
            radius={6}
            pathOptions={{ color: '#f97316', fillColor: '#f97316', fillOpacity: 0.6, weight: 2 }}
          >
            <Tooltip direction="top" offset={[0, -8]} permanent={false}>
              <span className="text-sm font-medium">{stop.stopName}</span>
              <br />
              <span className="text-xs text-gray-500">Route {stop.routeNumber}</span>
            </Tooltip>
          </CircleMarker>
        ))}

        {/* Bus markers */}
        {buses.map(bus => (
          <Marker
            key={bus.busId}
            position={[bus.lat || bus.latitude, bus.lng || bus.longitude]}
            icon={busIcon(bus.crowdLabel)}
          >
            <Popup>
              <div style={{ color: '#e2e8f0', minWidth: 150 }}>
                <b style={{ fontSize: 14 }}>{bus.busNumber}</b><br/>
                Occupancy: {bus.occupancyPercent ?? bus.occupancy ?? '--'}% {bus.crowdLabel || ''}<br/>
                ETA: {bus.etaMinutes != null ? `${Math.round(bus.etaMinutes)} min` : '--'}
              </div>
            </Popup>
          </Marker>
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
      </div>
    </div>
  )
}
