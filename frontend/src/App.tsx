import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout/Layout";
import DashboardPage from "./pages/DashboardPage";
import SourcesPage from "./pages/SourcesPage";
import MapPage from "./pages/MapPage";
import VulnerabilitiesPage from "./pages/VulnerabilitiesPage";
import ReportsPage from "./pages/ReportsPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/sources/*" element={<SourcesPage />} />
          <Route path="/map" element={<MapPage />} />
          <Route path="/vulnerabilities" element={<VulnerabilitiesPage />} />
          <Route path="/reports" element={<ReportsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
