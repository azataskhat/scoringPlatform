import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import { getDevicesForMap, getVulnerabilities } from "../../services/api";
import type { IoTDevice, Vulnerability } from "../../types";

// Fix Leaflet default icon
import "leaflet/dist/leaflet.css";

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: "#EF4444",
  HIGH: "#F59E0B",
  MEDIUM: "#3B82F6",
  LOW: "#10B981",
};

function createIcon(color: string) {
  return L.divIcon({
    className: "custom-marker",
    html: `<div style="
      width: 14px; height: 14px;
      background: ${color};
      border: 2px solid white;
      border-radius: 50%;
      box-shadow: 0 0 6px ${color};
    "></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
  });
}

export default function DeviceMap() {
  const [devices, setDevices] = useState<IoTDevice[]>([]);
  const [vulnMap, setVulnMap] = useState<Map<number, Vulnerability[]>>(new Map());

  useEffect(() => {
    getDevicesForMap().then(setDevices);
    getVulnerabilities().then((vulns) => {
      const map = new Map<number, Vulnerability[]>();
      vulns.forEach((v) => {
        const arr = map.get(v.deviceId) || [];
        arr.push(v);
        map.set(v.deviceId, arr);
      });
      setVulnMap(map);
    });
  }, []);

  const getDeviceColor = (deviceId: number): string => {
    const vulns = vulnMap.get(deviceId) || [];
    if (vulns.some((v) => v.severity === "CRITICAL")) return SEVERITY_COLORS.CRITICAL;
    if (vulns.some((v) => v.severity === "HIGH")) return SEVERITY_COLORS.HIGH;
    if (vulns.some((v) => v.severity === "MEDIUM")) return SEVERITY_COLORS.MEDIUM;
    return SEVERITY_COLORS.LOW;
  };

  // Center on Almaty
  const center: [number, number] = [43.2551, 76.9126];

  return (
    <div className="bg-card rounded-xl border border-border overflow-hidden" style={{ height: "calc(100vh - 180px)" }}>
      <MapContainer center={center} zoom={12} style={{ height: "100%", width: "100%" }} className="z-0">
        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />
        {devices.map((device) => (
          <Marker
            key={device.id}
            position={[device.latitude, device.longitude]}
            icon={createIcon(getDeviceColor(device.id))}
          >
            <Popup>
              <div className="text-sm text-gray-900 space-y-1">
                <p className="font-bold">{device.ipAddress}:{device.port}</p>
                <p>Protocol: {device.protocol}</p>
                <p>Type: {device.deviceType}</p>
                <p>Manufacturer: {device.manufacturer || "Unknown"}</p>
                <p>Vulnerabilities: {vulnMap.get(device.id)?.length || 0}</p>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      <div className="absolute bottom-6 right-6 bg-card/90 backdrop-blur border border-border rounded-lg p-3 z-[1000]">
        <p className="text-xs text-gray-400 mb-2 font-medium">Risk Level</p>
        {Object.entries(SEVERITY_COLORS).map(([label, color]) => (
          <div key={label} className="flex items-center gap-2 text-xs text-gray-300">
            <span className="w-3 h-3 rounded-full" style={{ backgroundColor: color }} />
            {label}
          </div>
        ))}
      </div>
    </div>
  );
}
