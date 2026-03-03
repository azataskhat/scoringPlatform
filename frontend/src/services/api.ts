import axios from "axios";
import type {
  OsintSource,
  IoTDevice,
  Vulnerability,
  ScoringResult,
  SecurityEvent,
  DashboardStats,
} from "../types";

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

// Sources
export const getSources = () => api.get<OsintSource[]>("/sources").then((r) => r.data);
export const getSource = (id: number) => api.get<OsintSource>(`/sources/${id}`).then((r) => r.data);
export const createSource = (data: Partial<OsintSource>) => api.post<OsintSource>("/sources", data).then((r) => r.data);
export const updateSource = (id: number, data: Partial<OsintSource>) => api.put<OsintSource>(`/sources/${id}`, data).then((r) => r.data);

// Devices
export const getDevices = (params?: { city?: string; type?: string; sourceId?: number }) =>
  api.get<IoTDevice[]>("/devices", { params }).then((r) => r.data);
export const getDevicesForMap = () => api.get<IoTDevice[]>("/devices/map").then((r) => r.data);

// Vulnerabilities
export const getVulnerabilities = (params?: { severity?: string; deviceId?: number; sourceId?: number }) =>
  api.get<Vulnerability[]>("/vulnerabilities", { params }).then((r) => r.data);

// Scoring
export const getScoringResults = (params?: { sourceId?: number }) =>
  api.get<ScoringResult[]>("/scoring/results", { params }).then((r) => r.data);
export const runScoring = () => api.post<ScoringResult[]>("/scoring/run").then((r) => r.data);
export const updateWeights = (weights: { reliabilityWeight: number; timelinessWeight: number; completenessWeight: number; accessibilityWeight: number }) =>
  api.put("/scoring/weights", weights);

// Dashboard
export const getDashboardStats = () => api.get<DashboardStats>("/dashboard/stats").then((r) => r.data);
export const getLatestEvents = (limit = 10) =>
  api.get<SecurityEvent[]>("/dashboard/events/latest", { params: { limit } }).then((r) => r.data);

// Reports
export const generateReport = (from: string, to: string) =>
  api.get("/reports/generate", { params: { from, to } }).then((r) => r.data);

export default api;
