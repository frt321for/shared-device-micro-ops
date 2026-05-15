import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import { Layout } from './components/layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import SitesPage from './pages/SitesPage';
import SiteDetailPage from './pages/SiteDetailPage';
import DevicesPage from './pages/DevicesPage';
import WorkOrdersPage from './pages/WorkOrdersPage';
import InventoryPage from './pages/InventoryPage';
import RoutePage from './pages/RoutePage';
import RevenuePage from './pages/RevenuePage';
import AiReportPage from './pages/AiReportPage';
import DeviceDetailPage from './pages/DeviceDetailPage';
import NotFoundPage from './pages/NotFoundPage';

function AppRoutes() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/" element={<DashboardPage />} />
        <Route path="/sites" element={<SitesPage />} />
        <Route path="/sites/:id" element={<SiteDetailPage />} />
        <Route path="/devices" element={<DevicesPage />} />
        <Route path="/devices/:id" element={<DeviceDetailPage />} />
        <Route path="/work-orders" element={<WorkOrdersPage />} />
        <Route path="/inventory" element={<InventoryPage />} />
        <Route path="/routes" element={<RoutePage />} />
        <Route path="/revenue" element={<RevenuePage />} />
        <Route path="/ai-report" element={<AiReportPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
