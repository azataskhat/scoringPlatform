import { useSSE } from "../../hooks/useSSE";

export default function Header() {
  const { connected } = useSSE("/api/scoring/stream");

  return (
    <header className="h-14 bg-gray-900 border-b border-border flex items-center justify-between px-6">
      <h2 className="text-sm font-medium text-gray-300">OSINT IoT Security Scoring Platform</h2>
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2 text-xs">
          <span className={`w-2 h-2 rounded-full ${connected ? "bg-green-500" : "bg-red-500"}`} />
          <span className="text-gray-400">{connected ? "SSE Connected" : "Disconnected"}</span>
        </div>
      </div>
    </header>
  );
}
