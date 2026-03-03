import {
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis,
  Radar, ResponsiveContainer, Tooltip,
} from "recharts";
import type { RadarDataPoint } from "../../types";

interface Props {
  data: RadarDataPoint[];
  title?: string;
}

export default function ScoringRadar({ data, title }: Props) {
  return (
    <div className="bg-card rounded-xl border border-border p-5">
      {title && <h3 className="text-sm font-medium text-gray-300 mb-4">{title}</h3>}
      <ResponsiveContainer width="100%" height={320}>
        <RadarChart data={data} cx="50%" cy="50%" outerRadius="75%">
          <PolarGrid stroke="#374151" />
          <PolarAngleAxis dataKey="dimension" tick={{ fill: "#9CA3AF", fontSize: 12 }} />
          <PolarRadiusAxis domain={[0, 1]} tick={{ fill: "#6B7280", fontSize: 10 }} />
          <Tooltip
            contentStyle={{ backgroundColor: "#1F2937", border: "1px solid #374151", borderRadius: 8 }}
          />
          <Radar
            name="Score"
            dataKey="value"
            stroke="#3B82F6"
            fill="#3B82F6"
            fillOpacity={0.3}
            strokeWidth={2}
          />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}
