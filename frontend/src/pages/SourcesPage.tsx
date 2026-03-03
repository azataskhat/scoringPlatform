import { useEffect, useState } from "react";
import { Routes, Route } from "react-router-dom";
import { getSources, getScoringResults, runScoring } from "../services/api";
import type { OsintSource, ScoringResult } from "../types";
import SourceList from "../components/Sources/SourceList";
import SourceDetail from "../components/Sources/SourceDetail";

function SourcesIndex() {
  const [sources, setSources] = useState<OsintSource[]>([]);
  const [scores, setScores] = useState<ScoringResult[]>([]);
  const [running, setRunning] = useState(false);

  const load = () => {
    getSources().then(setSources);
    getScoringResults().then(setScores);
  };

  useEffect(load, []);

  const handleRunScoring = async () => {
    setRunning(true);
    try {
      await runScoring();
      load();
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">OSINT Sources</h2>
        <button
          onClick={handleRunScoring}
          disabled={running}
          className="bg-primary hover:bg-blue-600 disabled:opacity-50 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          {running ? "Running..." : "Run Scoring"}
        </button>
      </div>
      <SourceList sources={sources} scores={scores} />
    </div>
  );
}

export default function SourcesPage() {
  return (
    <Routes>
      <Route index element={<SourcesIndex />} />
      <Route path=":id" element={<SourceDetail />} />
    </Routes>
  );
}
