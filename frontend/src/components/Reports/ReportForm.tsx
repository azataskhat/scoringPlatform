import { useState } from "react";
import { generateReport } from "../../services/api";

interface ReportData {
  period: { from: string; to: string };
  generatedAt: string;
  totalDevices: number;
  totalVulnerabilities: number;
  criticalVulnerabilities: number;
  averageReliability: number;
  averageTimeliness: number;
  averageCompleteness: number;
  averageAccessibility: number;
  averageScore: number;
  scoringDetails: Array<{
    sourceId: number;
    sourceName: string;
    totalScore: number;
    reliability: number;
    timeliness: number;
    completeness: number;
    accessibility: number;
    calculatedAt: string;
  }>;
}

export default function ReportForm() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [report, setReport] = useState<ReportData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleGenerate = async () => {
    if (!from || !to) return;
    setLoading(true);
    setError("");
    try {
      const data = await generateReport(
        new Date(from).toISOString(),
        new Date(to).toISOString()
      );
      setReport(data);
    } catch {
      setError("Failed to generate report. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (iso: string) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const scoreColor = (score: number) => {
    if (score >= 80) return "text-green-400";
    if (score >= 60) return "text-yellow-400";
    if (score >= 40) return "text-orange-400";
    return "text-red-400";
  };

  const scoreBg = (score: number) => {
    if (score >= 80) return "bg-green-500";
    if (score >= 60) return "bg-yellow-500";
    if (score >= 40) return "bg-orange-500";
    return "bg-red-500";
  };

  return (
    <div className="space-y-6">
      {/* Generate Form */}
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
            <svg className="w-5 h-5 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <div>
            <h3 className="text-sm font-semibold text-white">Generate Security Report</h3>
            <p className="text-xs text-gray-500">Select a date range to analyze</p>
          </div>
        </div>
        <div className="flex items-end gap-4 flex-wrap">
          <div className="flex-1 min-w-[160px]">
            <label className="block text-xs font-medium text-gray-400 mb-1.5">From</label>
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="w-full bg-gray-800 border border-border rounded-lg px-3 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-colors"
            />
          </div>
          <div className="flex-1 min-w-[160px]">
            <label className="block text-xs font-medium text-gray-400 mb-1.5">To</label>
            <input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="w-full bg-gray-800 border border-border rounded-lg px-3 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-colors"
            />
          </div>
          <button
            onClick={handleGenerate}
            disabled={loading || !from || !to}
            className="bg-primary hover:bg-blue-600 disabled:opacity-40 disabled:cursor-not-allowed text-white px-8 py-2.5 rounded-lg text-sm font-medium transition-all active:scale-95 flex items-center gap-2"
          >
            {loading ? (
              <>
                <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" className="opacity-25" />
                  <path d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="3" strokeLinecap="round" className="opacity-75" />
                </svg>
                Generating...
              </>
            ) : (
              "Generate Report"
            )}
          </button>
        </div>
        {error && (
          <p className="mt-3 text-sm text-red-400">{error}</p>
        )}
      </div>

      {report && (
        <>
          {/* Report Header */}
          <div className="bg-card rounded-xl border border-border p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-bold text-white">Security Assessment Report</h3>
                <p className="text-xs text-gray-500 mt-1">
                  {formatDate(report.period?.from)} — {formatDate(report.period?.to)}
                </p>
              </div>
              <div className="text-right">
                <p className="text-xs text-gray-500">Generated</p>
                <p className="text-xs text-gray-400">{formatDate(report.generatedAt)}</p>
              </div>
            </div>

            {/* Summary Stats */}
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
              <StatCard
                label="Overall Score"
                value={report.averageScore?.toFixed(1) ?? "—"}
                color={scoreColor(report.averageScore ?? 0)}
                bg="bg-blue-500/10"
                large
              />
              <StatCard
                label="Total Devices"
                value={String(report.totalDevices ?? 0)}
                color="text-cyan-400"
                bg="bg-cyan-500/10"
              />
              <StatCard
                label="Vulnerabilities"
                value={String(report.totalVulnerabilities ?? 0)}
                color="text-yellow-400"
                bg="bg-yellow-500/10"
              />
              <StatCard
                label="Critical"
                value={String(report.criticalVulnerabilities ?? 0)}
                color="text-red-400"
                bg="bg-red-500/10"
              />
              <StatCard
                label="Sources Scored"
                value={String(report.scoringDetails?.length ?? 0)}
                color="text-purple-400"
                bg="bg-purple-500/10"
              />
            </div>
          </div>

          {/* Scoring Dimensions */}
          <div className="bg-card rounded-xl border border-border p-6">
            <h4 className="text-sm font-semibold text-white mb-4">Average Scoring Dimensions</h4>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <DimensionBar label="Reliability" value={report.averageReliability ?? 0} />
              <DimensionBar label="Timeliness" value={report.averageTimeliness ?? 0} />
              <DimensionBar label="Completeness" value={report.averageCompleteness ?? 0} />
              <DimensionBar label="Accessibility" value={report.averageAccessibility ?? 0} />
            </div>
          </div>

          {/* Source Scoring Details */}
          {report.scoringDetails && report.scoringDetails.length > 0 && (
            <div className="bg-card rounded-xl border border-border p-6">
              <h4 className="text-sm font-semibold text-white mb-4">Source Scoring Breakdown</h4>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-xs text-gray-500 uppercase tracking-wider border-b border-border">
                      <th className="text-left pb-3 pr-4">Source</th>
                      <th className="text-center pb-3 px-2">Total</th>
                      <th className="text-center pb-3 px-2">Reliability</th>
                      <th className="text-center pb-3 px-2">Timeliness</th>
                      <th className="text-center pb-3 px-2">Completeness</th>
                      <th className="text-center pb-3 px-2">Accessibility</th>
                      <th className="text-right pb-3 pl-4">Scored At</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {report.scoringDetails.map((detail, i) => (
                      <tr key={i} className="hover:bg-gray-800/50 transition-colors">
                        <td className="py-3 pr-4">
                          <span className="font-medium text-white">
                            {detail.sourceName || `Source #${detail.sourceId}`}
                          </span>
                        </td>
                        <td className="py-3 px-2 text-center">
                          <span className={`font-bold text-base ${scoreColor(detail.totalScore)}`}>
                            {detail.totalScore?.toFixed(1)}
                          </span>
                        </td>
                        <td className="py-3 px-2">
                          <MiniBar value={detail.reliability} />
                        </td>
                        <td className="py-3 px-2">
                          <MiniBar value={detail.timeliness} />
                        </td>
                        <td className="py-3 px-2">
                          <MiniBar value={detail.completeness} />
                        </td>
                        <td className="py-3 px-2">
                          <MiniBar value={detail.accessibility} />
                        </td>
                        <td className="py-3 pl-4 text-right text-xs text-gray-500">
                          {formatDate(detail.calculatedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Raw JSON (collapsible) */}
          <details className="bg-card rounded-xl border border-border">
            <summary className="px-6 py-4 cursor-pointer text-sm text-gray-400 hover:text-gray-300 transition-colors select-none flex items-center gap-2">
              <svg className="w-4 h-4 transition-transform details-open:rotate-90" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
              Raw JSON Data
            </summary>
            <div className="px-6 pb-4">
              <pre className="bg-gray-800/50 rounded-lg p-4 overflow-auto max-h-80 text-xs text-gray-400 whitespace-pre-wrap">
                {JSON.stringify(report, null, 2)}
              </pre>
            </div>
          </details>
        </>
      )}
    </div>
  );
}

function StatCard({ label, value, color, bg, large }: {
  label: string;
  value: string;
  color: string;
  bg: string;
  large?: boolean;
}) {
  return (
    <div className={`${bg} rounded-lg p-4 border border-border/50`}>
      <p className="text-xs text-gray-400 mb-1">{label}</p>
      <p className={`${large ? "text-3xl" : "text-2xl"} font-bold ${color}`}>{value}</p>
    </div>
  );
}

function DimensionBar({ label, value }: { label: string; value: number }) {
  const pct = Math.min(100, Math.max(0, value));
  const color =
    pct >= 80 ? "bg-green-500" : pct >= 60 ? "bg-yellow-500" : pct >= 40 ? "bg-orange-500" : "bg-red-500";
  const textColor =
    pct >= 80 ? "text-green-400" : pct >= 60 ? "text-yellow-400" : pct >= 40 ? "text-orange-400" : "text-red-400";

  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <span className="text-xs text-gray-400">{label}</span>
        <span className={`text-sm font-bold ${textColor}`}>{pct.toFixed(1)}</span>
      </div>
      <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full ${color} transition-all duration-700 ease-out`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function MiniBar({ value }: { value: number }) {
  const pct = Math.min(100, Math.max(0, value ?? 0));
  const color =
    pct >= 80 ? "bg-green-500" : pct >= 60 ? "bg-yellow-500" : pct >= 40 ? "bg-orange-500" : "bg-red-500";
  const textColor =
    pct >= 80 ? "text-green-400" : pct >= 60 ? "text-yellow-400" : pct >= 40 ? "text-orange-400" : "text-red-400";

  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-1.5 bg-gray-800 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className={`text-xs font-medium ${textColor} w-8 text-right`}>{pct.toFixed(0)}</span>
    </div>
  );
}
