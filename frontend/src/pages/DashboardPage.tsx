import { useEffect, useState } from "react";
import { getDashboardStats, getScoringResults, getLatestEvents } from "../services/api";
import { useSSE } from "../hooks/useSSE";
import type { DashboardStats, ScoringResult, SecurityEvent } from "../types";
import StatsCards from "../components/Dashboard/StatsCards";
import ScoreTrend from "../components/Dashboard/ScoreTrend";

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [scores, setScores] = useState<ScoringResult[]>([]);
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const { data: sseUpdate } = useSSE("/api/scoring/stream");

  useEffect(() => {
    getDashboardStats().then(setStats);
    getScoringResults().then(setScores);
    getLatestEvents(10).then(setEvents);
  }, []);

  // Refresh on SSE update
  useEffect(() => {
    if (sseUpdate) {
      getDashboardStats().then(setStats);
      getScoringResults().then(setScores);
    }
  }, [sseUpdate]);

  if (!stats) return <div className="text-gray-400">Loading dashboard...</div>;

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold">Dashboard</h2>

      <StatsCards stats={stats} />

      <ScoreTrend results={scores} />

      <div className="bg-card rounded-xl border border-border p-5">
        <h3 className="text-sm font-medium text-gray-300 mb-4">Latest Security Events</h3>
        <div className="space-y-2">
          {events.map((event) => (
            <div
              key={event.id}
              className="flex items-center justify-between p-3 bg-gray-800/50 rounded-lg"
            >
              <div className="flex items-center gap-3">
                <span
                  className={`px-2 py-0.5 rounded text-xs font-medium ${
                    event.severity === "CRITICAL"
                      ? "bg-red-500/20 text-red-400"
                      : event.severity === "HIGH"
                      ? "bg-orange-500/20 text-orange-400"
                      : event.severity === "MEDIUM"
                      ? "bg-yellow-500/20 text-yellow-400"
                      : "bg-green-500/20 text-green-400"
                  }`}
                >
                  {event.severity}
                </span>
                <span className="text-sm text-gray-300">{event.description}</span>
              </div>
              <span className="text-xs text-gray-500">
                {event.eventTime ? new Date(event.eventTime).toLocaleString("ru-RU") : ""}
              </span>
            </div>
          ))}
          {events.length === 0 && (
            <p className="text-gray-500 text-sm text-center py-4">No events yet</p>
          )}
        </div>
      </div>
    </div>
  );
}
