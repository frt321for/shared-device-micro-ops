import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './hooks/useAuth';
import { Layout } from './components/layout';
import DashboardPage from './pages/DashboardPage';
import SitesPage from './pages/SitesPage';
import DevicesPage from './pages/DevicesPage';
import WorkOrdersPage from './pages/WorkOrdersPage';
import InventoryPage from './pages/InventoryPage';
import RoutePage from './pages/RoutePage';
import RevenuePage from './pages/RevenuePage';
import AiReportPage from './pages/AiReportPage';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/sites" element={<SitesPage />} />
            <Route path="/devices" element={<DevicesPage />} />
            <Route path="/work-orders" element={<WorkOrdersPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/routes" element={<RoutePage />} />
            <Route path="/revenue" element={<RevenuePage />} />
            <Route path="/ai-reports" element={<AiReportPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
