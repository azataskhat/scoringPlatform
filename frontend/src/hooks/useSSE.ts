import { useEffect, useState, useRef } from "react";
import type { ScoringResult } from "../types";

export function useSSE(url: string) {
  const [data, setData] = useState<ScoringResult | null>(null);
  const [connected, setConnected] = useState(false);
  const sourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    const es = new EventSource(url);
    sourceRef.current = es;

    es.onopen = () => setConnected(true);

    es.addEventListener("scoring-update", (e: MessageEvent) => {
      try {
        setData(JSON.parse(e.data));
      } catch {
        // ignore parse errors
      }
    });

    es.onerror = () => {
      setConnected(false);
    };

    return () => {
      es.close();
      sourceRef.current = null;
    };
  }, [url]);

  return { data, connected };
}
