import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

// Buses
export const getAllBuses = () => api.get('/buses').then(r => r.data.data)
export const getBusById = (id) => api.get(`/buses/${id}`).then(r => r.data.data)

// Routes
export const getAllRoutes = () => api.get('/routes').then(r => r.data.data)
export const getStopsByRoute = (routeId) => api.get(`/routes/${routeId}/stops`).then(r => r.data.data)

// Commuter
export const getNearbyStops = (lat, lng, radius = 500) =>
  api.get('/commuter/stops/nearby', { params: { lat, lng, radiusMeters: radius } }).then(r => r.data.data)

export const getAllStops = (lat = 18.5204, lng = 73.8567) =>
  api.get('/commuter/stops', { params: { lat, lng } }).then(r => r.data.data)

export const getIncomingBuses = (stopId) =>
  api.get(`/commuter/stops/${stopId}/incoming-buses`).then(r => r.data.data)

export const getStopAlerts = (stopId, last = 10) =>
  api.get(`/commuter/stops/${stopId}/alerts`, { params: { last } }).then(r => r.data.data)

// Occupancy
export const updateOccupancy = (data) =>
  api.post('/occupancy/update', data).then(r => r.data.data)

export const getCurrentOccupancy = (busId) =>
  api.get(`/occupancy/${busId}/current`).then(r => r.data.data)

export default api
