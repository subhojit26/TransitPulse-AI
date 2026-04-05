import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import Home from './pages/Home'
import StopDetail from './pages/StopDetail'
import LiveMap from './pages/LiveMap'
import Conductor from './pages/Conductor'
import Admin from './pages/Admin'

export default function App() {
  return (
    <div className="min-h-screen bg-[#0f172a]">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 py-6">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/stop/:stopId" element={<StopDetail />} />
          <Route path="/map" element={<LiveMap />} />
          <Route path="/conductor" element={<Conductor />} />
          <Route path="/admin" element={<Admin />} />
        </Routes>
      </main>
    </div>
  )
}
