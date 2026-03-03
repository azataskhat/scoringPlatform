import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend,
} from "recharts";
import type { ScoringResult } from "../../types";

interface Props {
  results: ScoringResult[];
}

export default function ScoreTrend({ results }: Props) {
  const chartData = results.map((r) => ({
    date: new Date(r.calculatedAt).toLocaleDateString("ru-RU"),
    sourceId: r.sourceId,
    total: +(r.totalScore?.toFixed(3) ?? 0),
    reliability: +(r.reliabilityScore?.toFixed(3) ?? 0),
    timeliness: +(r.timelinessScore?.toFixed(3) ?? 0),
    completeness: +(r.completenessScore?.toFixed(3) ?? 0),
    accessibility: +(r.accessibilityScore?.toFixed(3) ?? 0),
  }));

  return (
    <div className="bg-card rounded-xl border border-border p-5">
      <h3 className="text-sm font-medium text-gray-300 mb-4">Scoring Trend</h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
          <XAxis dataKey="date" stroke="#6B7280" fontSize={12} />
          <YAxis domain={[0, 1]} stroke="#6B7280" fontSize={12} />
          <Tooltip
            contentStyle={{ backgroundColor: "#1F2937", border: "1px solid #374151", borderRadius: 8 }}
            labelStyle={{ color: "#9CA3AF" }}
          />
          <Legend />
          <Line type="monotone" dataKey="total" stroke="#3B82F6" strokeWidth={2} name="Total" dot={false} />
          <Line type="monotone" dataKey="reliability" stroke="#10B981" strokeWidth={1} name="R" dot={false} />
          <Line type="monotone" dataKey="timeliness" stroke="#F59E0B" strokeWidth={1} name="T" dot={false} />
          <Line type="monotone" dataKey="completeness" stroke="#8B5CF6" strokeWidth={1} name="C" dot={false} />
          <Line type="monotone" dataKey="accessibility" stroke="#EC4899" strokeWidth={1} name="A" dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
