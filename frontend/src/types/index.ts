export interface OsintSource {
  id: number;
  name: string;
  type: string;
  baseUrl: string;
  apiKeyRef: string;
  active: boolean;
  updateIntervalMinutes: number;
  createdAt: string;
  updatedAt: string;
}

export interface IoTDevice {
  id: number;
  sourceId: number;
  ipAddress: string;
  port: number;
  protocol: string;
  deviceType: string;
  manufacturer: string;
  firmwareVersion: string;
  city: string;
  latitude: number;
  longitude: number;
  rawData: string;
  discoveredAt: string;
}

export interface Vulnerability {
  id: number;
  deviceId: number;
  cveId: string;
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  cvssScore: number;
  description: string;
  sourceId: number;
  detectedAt: string;
}

export interface ScoringResult {
  id: number;
  sourceId: number;
  reliabilityScore: number;
  timelinessScore: number;
  completenessScore: number;
  accessibilityScore: number;
  totalScore: number;
  parameters: string;
  calculatedAt: string;
}

export interface ScoringParameters {
  r1: number; r2: number; r3: number;
  t1: number; t2: number;
  c1: number; c2: number; c3: number;
  a1: number; a2: number;
}

export interface SecurityEvent {
  id: number;
  deviceId: number;
  eventType: string;
  severity: string;
  description: string;
  sourceId: number;
  eventTime: string;
}

export interface DashboardStats {
  totalSources: number;
  activeSources: number;
  totalDevices: number;
  totalVulnerabilities: number;
  criticalVulnerabilities: number;
  totalEvents: number;
  averageScore: number;
}

export interface RadarDataPoint {
  dimension: string;
  value: number;
  fullMark: number;
}
