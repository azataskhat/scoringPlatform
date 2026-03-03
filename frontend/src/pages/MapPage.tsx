import DeviceMap from "../components/Map/DeviceMap";

export default function MapPage() {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">IoT Device Map</h2>
      <div className="relative">
        <DeviceMap />
      </div>
    </div>
  );
}
