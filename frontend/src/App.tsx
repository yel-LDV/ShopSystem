import { lazy, Suspense } from 'react'
import type { ReactNode } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import AppShell from './components/layout/AppShell'

const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'))
const RegistrationRequests = lazy(() => import('./pages/admin/RegistrationRequests'))
const AuditLogPage = lazy(() => import('./pages/admin/AuditLogPage'))
const BackupRestore = lazy(() => import('./pages/admin/BackupRestore'))
const AdminTicketsPage = lazy(() => import('./pages/admin/AdminTicketsPage'))
const StoreDashboard = lazy(() => import('./pages/store/StoreDashboard'))
const InventoryList = lazy(() => import('./pages/store/InventoryList'))
const NewOrder = lazy(() => import('./pages/store/NewOrder'))
const OrderList = lazy(() => import('./pages/store/OrderList'))
const PosPage = lazy(() => import('./pages/store/PosPage'))
const StoreTicketsPage = lazy(() => import('./pages/store/StoreTicketsPage'))
const SupplierDashboard = lazy(() => import('./pages/supplier/SupplierDashboard'))
const ProductList = lazy(() => import('./pages/supplier/ProductList'))
const SupplierOrders = lazy(() => import('./pages/supplier/SupplierOrders'))
const SupplierTicketsPage = lazy(() => import('./pages/supplier/SupplierTicketsPage'))
const TicketChatPage = lazy(() => import('./pages/TicketChatPage'))

function Loading() {
  return (
    <div className="flex items-center justify-center h-full">
      <div className="w-6 h-6 border-2 border-accent/30 border-t-accent rounded-full animate-spin" />
    </div>
  )
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const token = sessionStorage.getItem('token')
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return (
    <AppShell>
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </AppShell>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register/:type" element={<RegisterPage />} />

      <Route path="/admin" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
      <Route path="/admin/registrations" element={<ProtectedRoute><RegistrationRequests /></ProtectedRoute>} />
      <Route path="/admin/audit" element={<ProtectedRoute><AuditLogPage /></ProtectedRoute>} />
      <Route path="/admin/backup" element={<ProtectedRoute><BackupRestore /></ProtectedRoute>} />
      <Route path="/admin/tickets" element={<ProtectedRoute><AdminTicketsPage /></ProtectedRoute>} />

      <Route path="/store" element={<ProtectedRoute><StoreDashboard /></ProtectedRoute>} />
      <Route path="/store/inventory" element={<ProtectedRoute><InventoryList /></ProtectedRoute>} />
      <Route path="/store/new-order" element={<ProtectedRoute><NewOrder /></ProtectedRoute>} />
      <Route path="/store/orders" element={<ProtectedRoute><OrderList /></ProtectedRoute>} />
      <Route path="/store/pos" element={<ProtectedRoute><PosPage /></ProtectedRoute>} />
      <Route path="/store/tickets" element={<ProtectedRoute><StoreTicketsPage /></ProtectedRoute>} />

      <Route path="/supplier" element={<ProtectedRoute><SupplierDashboard /></ProtectedRoute>} />
      <Route path="/supplier/products" element={<ProtectedRoute><ProductList /></ProtectedRoute>} />
      <Route path="/supplier/orders" element={<ProtectedRoute><SupplierOrders /></ProtectedRoute>} />
      <Route path="/supplier/tickets" element={<ProtectedRoute><SupplierTicketsPage /></ProtectedRoute>} />

      <Route path="/ticket/:id" element={<ProtectedRoute><TicketChatPage /></ProtectedRoute>} />

      <Route path="*" element={<Navigate to="/login" />} />
    </Routes>
  )
}
