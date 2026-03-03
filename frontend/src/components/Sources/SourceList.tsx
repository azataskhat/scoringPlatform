import { Link } from "react-router-dom";
import type { OsintSource, ScoringResult } from "../../types";

interface Props {
  sources: OsintSource[];
  scores: ScoringResult[];
}

export default function SourceList({ sources, scores }: Props) {
  const latestScores = new Map<number, ScoringResult>();
  scores.forEach((s) => {
    if (!latestScores.has(s.sourceId)) latestScores.set(s.sourceId, s);
  });

  return (
    <div className="bg-card rounded-xl border border-border overflow-hidden">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border text-gray-400 text-xs uppercase">
            <th className="text-left p-4">Source</th>
            <th className="text-left p-4">Type</th>
            <th className="text-center p-4">Status</th>
            <th className="text-center p-4">Interval</th>
            <th className="text-center p-4">R</th>
            <th className="text-center p-4">T</th>
            <th className="text-center p-4">C</th>
            <th className="text-center p-4">A</th>
            <th className="text-center p-4">Total</th>
          </tr>
        </thead>
        <tbody>
          {sources.map((src) => {
            const score = latestScores.get(src.id);
            return (
              <tr key={src.id} className="border-b border-border/50 hover:bg-gray-800/50 transition-colors">
                <td className="p-4">
                  <Link to={`/sources/${src.id}`} className="text-primary hover:underline font-medium">
                    {src.name}
                  </Link>
                </td>
                <td className="p-4 text-gray-400">{src.type}</td>
                <td className="p-4 text-center">
                  <span className={`px-2 py-1 rounded-full text-xs ${src.active ? "bg-green-500/20 text-green-400" : "bg-red-500/20 text-red-400"}`}>
                    {src.active ? "Active" : "Inactive"}
                  </span>
                </td>
                <td className="p-4 text-center text-gray-400">{src.updateIntervalMinutes}m</td>
                <td className="p-4 text-center">{score?.reliabilityScore?.toFixed(2) ?? "—"}</td>
                <td className="p-4 text-center">{score?.timelinessScore?.toFixed(2) ?? "—"}</td>
                <td className="p-4 text-center">{score?.completenessScore?.toFixed(2) ?? "—"}</td>
                <td className="p-4 text-center">{score?.accessibilityScore?.toFixed(2) ?? "—"}</td>
                <td className="p-4 text-center">
                  <span className="text-primary font-bold">{score?.totalScore?.toFixed(3) ?? "—"}</span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
