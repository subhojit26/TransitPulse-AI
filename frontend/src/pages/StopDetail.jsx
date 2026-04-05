import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { getStopAlerts } from '../api'
import { createStompClient } from '../stomp'
import OccupancyBar from '../components/OccupancyBar'
import StatusBadge from '../components/StatusBadge'
import EtaBadge from '../components/EtaBadge'

export default function StopDetail() {
  const { stopId } = useParams()
  const [data, setData] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [connected, setConnected] = useState(false)
  const clientRef = useRef(null)

  useEffect(() => {
    // Load alerts
    getStopAlerts(stopId, 5).then(setAlerts).catch(() => {})

    // WebSocket connection
    const client = createStompClient((c) => {
      setConnected(true)
      c.subscribe(`/topic/stops/${stopId}`, (msg) => {
        const update = JSON.parse(msg.body)
        setData(update)
        if (update.alerts?.length) setAlerts(update.alerts.slice(0, 5))
      })
    })
    clientRef.current = client

    return () => {
      if (clientRef.current) clientRef.current.deactivate()
    }
  }, [stopId])

  return (
    <div className="max-w-3xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <div className="flex items-center gap-2">
          <span className={`w-2.5 h-2.5 rounded-full ${connected ? 'bg-green-400 animate-pulse-dot' : 'bg-red-400'}`} />
          <h1 className="text-2xl font-bold text-white">
            {data?.stopName || `Stop #${stopId}`}
          </h1>
        </div>
        <span className="text-xs text-slate-500">
          {connected ? 'LIVE' : 'Connecting...'}
        </span>
      </div>

      {/* Bus list */}
      <div className="space-y-3 mb-8">
        {!data && (
          <div className="bg-[#1e293b] border border-[#334155] rounded-lg p-8 text-center text-slate-500">
            Waiting for live data...
          </div>
        )}
        {data?.buses?.map((bus, i) => (
          <div key={i} className="bg-[#1e293b] border border-[#334155] rounded-lg p-4">
            <div className="flex items-start justify-between mb-3">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-lg font-bold text-orange-400">{bus.busNumber}</span>
                  <span className="text-sm text-slate-400">Route {bus.routeNumber}</span>
                  <StatusBadge status={bus.status} />
                </div>
              </div>
            </div>

            {/* ETA display */}
            <div className="grid grid-cols-2 gap-3 mb-3">
              <div className="bg-[#0f172a] rounded p-3">
                <p className="text-xs text-slate-500 mb-1">Stream ETA</p>
                <EtaBadge eta={bus.streamEtaMinutes} />
              </div>
              <div className="bg-[#0f172a] rounded p-3">
                <p className="text-xs text-slate-500 mb-1">AI Adjusted ETA</p>
                <div className="flex items-center gap-2">
                  <EtaBadge eta={bus.aiAdjustedEtaMinutes} />
                  {bus.aiConfidence != null && (
                    <span className="text-xs text-slate-500">
                      {Math.round(bus.aiConfidence * 100)}% conf
                    </span>
                  )}
                </div>
              </div>
            </div>

            {bus.aiReason && (
              <p className="text-xs italic text-slate-500 mb-3">
                AI: {bus.aiReason}
              </p>
            )}

            <OccupancyBar percent={bus.occupancyPercent} label={bus.crowdLabel} />
          </div>
        ))}
      </div>

      {/* Alerts */}
      <div>
        <h2 className="text-lg font-semibold text-white mb-3">Recent Alerts</h2>
        {alerts.length === 0 ? (
          <p className="text-sm text-slate-500">No alerts for this stop.</p>
        ) : (
          <div className="space-y-2">
            {alerts.map((alert, i) => (
              <div key={alert.id || i} className="bg-[#1e293b] border border-[#334155] rounded-lg p-3 flex items-start gap-3">
                <span className={`mt-0.5 text-sm ${
                  alert.alertType === 'BREAKDOWN_ALERT' ? 'text-red-400' :
                  alert.alertType === 'CROWD_ALERT' ? 'text-orange-400' :
                  alert.alertType === 'DELAY_ALERT' ? 'text-yellow-400' :
                  'text-blue-400'
                }`}>
                  {alert.alertType === 'BREAKDOWN_ALERT' ? '⚠️' :
                   alert.alertType === 'CROWD_ALERT' ? '👥' :
                   alert.alertType === 'DELAY_ALERT' ? '⏱️' : '🚌'}
                </span>
                <div>
                  <p className="text-sm text-slate-300">{alert.message}</p>
                  <p className="text-xs text-slate-600 mt-1">
                    {alert.createdAt ? new Date(alert.createdAt).toLocaleTimeString() : ''}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
