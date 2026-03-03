import type { DashboardStats } from "../../types";

interface Props {
  stats: DashboardStats;
}

const cards = [
  { key: "activeSources" as const, label: "Active Sources", color: "text-primary", bg: "bg-blue-500/10" },
  { key: "averageScore" as const, label: "Avg Score", color: "text-green-400", bg: "bg-green-500/10" },
  { key: "totalDevices" as const, label: "IoT Devices", color: "text-yellow-400", bg: "bg-yellow-500/10" },
  { key: "totalVulnerabilities" as const, label: "Vulnerabilities", color: "text-red-400", bg: "bg-red-500/10" },
];

export default function StatsCards({ stats }: Props) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const value = stats[card.key];
        return (
          <div key={card.key} className={`${card.bg} rounded-xl p-5 border border-border`}>
            <p className="text-xs text-gray-400 uppercase tracking-wider mb-1">{card.label}</p>
            <p className={`text-3xl font-bold ${card.color}`}>
              {typeof value === "number" && card.key === "averageScore"
                ? value.toFixed(2)
                : value}
            </p>
            {card.key === "totalVulnerabilities" && (
              <p className="text-xs text-red-400 mt-1">
                {stats.criticalVulnerabilities} critical
              </p>
            )}
          </div>
        );
      })}
    </div>
  );
}
