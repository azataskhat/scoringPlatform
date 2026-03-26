import { useEffect, useState } from "react";
import { getVulnerabilities } from "../../services/api";
import type { Vulnerability } from "../../types";
import { formatDate } from "../../utils/dateFormat";

const SEVERITY_STYLES: Record<string, string> = {
  CRITICAL: "bg-red-500/20 text-red-400",
  HIGH: "bg-orange-500/20 text-orange-400",
  MEDIUM: "bg-yellow-500/20 text-yellow-400",
  LOW: "bg-green-500/20 text-green-400",
};

export default function VulnTable() {
  const [vulns, setVulns] = useState<Vulnerability[]>([]);
  const [filter, setFilter] = useState<string>("ALL");

  useEffect(() => {
    const params = filter !== "ALL" ? { severity: filter } : undefined;
    getVulnerabilities(params).then(setVulns);
  }, [filter]);

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        {["ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"].map((sev) => (
          <button
            key={sev}
            onClick={() => setFilter(sev)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              filter === sev
                ? "bg-primary text-white"
                : "bg-gray-800 text-gray-400 hover:bg-gray-700"
            }`}
          >
            {sev}
          </button>
        ))}
      </div>

      <div className="bg-card rounded-xl border border-border overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-gray-400 text-xs uppercase">
              <th className="text-left p-4">CVE ID</th>
              <th className="text-center p-4">Severity</th>
              <th className="text-center p-4">CVSS</th>
              <th className="text-left p-4">Description</th>
              <th className="text-center p-4">Detected</th>
            </tr>
          </thead>
          <tbody>
            {vulns.map((v) => (
              <tr key={v.id} className="border-b border-border/50 hover:bg-gray-800/50">
                <td className="p-4 font-mono text-primary">{v.cveId}</td>
                <td className="p-4 text-center">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${SEVERITY_STYLES[v.severity] ?? ""}`}>
                    {v.severity}
                  </span>
                </td>
                <td className="p-4 text-center font-bold">
                  {v.cvssScore?.toFixed(1) ?? "—"}
                </td>
                <td className="p-4 text-gray-300 max-w-md truncate">{v.description}</td>
                <td className="p-4 text-center text-gray-400 text-xs">
                  {formatDate(v.detectedAt)}
                </td>
              </tr>
            ))}
            {vulns.length === 0 && (
              <tr>
                <td colSpan={5} className="p-8 text-center text-gray-500">No vulnerabilities found</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
