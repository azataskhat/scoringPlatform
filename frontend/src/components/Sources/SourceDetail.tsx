import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getSource, getScoringResults } from "../../services/api";
import type { OsintSource, ScoringResult, RadarDataPoint } from "../../types";
import ScoringRadar from "./ScoringRadar";
import ScoreTrend from "../Dashboard/ScoreTrend";

export default function SourceDetail() {
  const { id } = useParams<{ id: string }>();
  const [source, setSource] = useState<OsintSource | null>(null);
  const [scores, setScores] = useState<ScoringResult[]>([]);

  useEffect(() => {
    if (!id) return;
    getSource(Number(id)).then(setSource);
    getScoringResults({ sourceId: Number(id) }).then(setScores);
  }, [id]);

  if (!source) return <div className="text-gray-400">Loading...</div>;

  const latest = scores.length > 0 ? scores[scores.length - 1] : undefined;
  const radarData: RadarDataPoint[] = latest
    ? [
        { dimension: "Reliability (R)", value: latest.reliabilityScore, fullMark: 1 },
        { dimension: "Timeliness (T)", value: latest.timelinessScore, fullMark: 1 },
        { dimension: "Completeness (C)", value: latest.completenessScore, fullMark: 1 },
        { dimension: "Accessibility (A)", value: latest.accessibilityScore, fullMark: 1 },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">{source.name}</h2>
          <p className="text-gray-400 text-sm mt-1">{source.type} &middot; {source.baseUrl}</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-gray-400">Total Score</p>
          <p className="text-4xl font-bold text-primary">{latest?.totalScore?.toFixed(3) ?? "—"}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ScoringRadar data={radarData} title="Scoring Dimensions" />

        <div className="bg-card rounded-xl border border-border p-5">
          <h3 className="text-sm font-medium text-gray-300 mb-4">Score Details</h3>
          {latest && (
            <div className="space-y-3">
              {[
                { label: "Reliability (R)", value: latest.reliabilityScore, color: "bg-green-500" },
                { label: "Timeliness (T)", value: latest.timelinessScore, color: "bg-yellow-500" },
                { label: "Completeness (C)", value: latest.completenessScore, color: "bg-purple-500" },
                { label: "Accessibility (A)", value: latest.accessibilityScore, color: "bg-pink-500" },
              ].map((item) => (
                <div key={item.label}>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-gray-400">{item.label}</span>
                    <span className="text-white font-medium">{item.value?.toFixed(3)}</span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-2">
                    <div
                      className={`${item.color} h-2 rounded-full transition-all`}
                      style={{ width: `${(item.value ?? 0) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <ScoreTrend results={scores} />
    </div>
  );
}
