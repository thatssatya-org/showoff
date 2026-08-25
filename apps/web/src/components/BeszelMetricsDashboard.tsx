import { useEffect, useState } from "react";

type MetricSystem = Readonly<{
  name: string;
  state: string;
  observedAt: string | null;
  cpuPercent: number | null;
  memoryPercent: number | null;
  diskPercent: number | null;
  loadAverage: number | null;
}>;

type MetricsResponse = Readonly<{
  refreshedAt: string;
  stale: boolean;
  systems: readonly MetricSystem[];
}>;

const ENDPOINT = "/operator/beszel/metrics";
const REQUEST_TIMEOUT_MS = 6_000;

function isMetricsResponse(value: unknown): value is MetricsResponse {
  if (typeof value !== "object" || value === null) return false;
  const response = value as Record<string, unknown>;
  return typeof response.refreshedAt === "string" && typeof response.stale === "boolean" && Array.isArray(response.systems);
}

function percentage(value: number | null): string {
  return typeof value === "number" && Number.isFinite(value) ? `${value.toFixed(1)}%` : "—";
}

function load(value: number | null): string {
  return typeof value === "number" && Number.isFinite(value) ? value.toFixed(2) : "—";
}

function timestamp(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? "Refresh time unavailable" : new Intl.DateTimeFormat("en", {
    month: "short", day: "numeric", hour: "2-digit", minute: "2-digit", timeZoneName: "short"
  }).format(date);
}

export default function BeszelMetricsDashboard() {
  const [metrics, setMetrics] = useState<MetricsResponse | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    const timeout = globalThis.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    void fetch(ENDPOINT, { headers: { Accept: "application/json" }, cache: "no-store", signal: controller.signal })
      .then(async (response) => response.ok ? response.json() : Promise.reject(new Error("metrics unavailable")))
      .then((body: unknown) => {
        if (!isMetricsResponse(body)) throw new Error("invalid metrics response");
        setMetrics(body);
      })
      .catch(() => setFailed(true))
      .finally(() => globalThis.clearTimeout(timeout));
    return () => {
      controller.abort();
      globalThis.clearTimeout(timeout);
    };
  }, []);

  if (failed) return <p className="beszel-dashboard__notice">Metrics are unavailable. Pair a valid token and confirm the backend Tailnet origin is reachable.</p>;
  if (metrics === null) return <p className="beszel-dashboard__notice">Loading backend-projected metrics…</p>;
  if (metrics.systems.length === 0) return <p className="beszel-dashboard__notice">No Beszel systems are visible to this token.</p>;

  return <section className="beszel-dashboard" aria-label="Beszel metric systems">
    <p className="beszel-dashboard__status">{metrics.stale ? "Last known good metrics" : "Backend-projected metrics"} · {timestamp(metrics.refreshedAt)}</p>
    <div className="beszel-dashboard__grid">
      {metrics.systems.map((system) => <article className="beszel-system card" key={system.name}>
        <header>
          <div><p className="source-meta">Beszel / {system.state}</p><h2>{system.name}</h2></div>
          <span className="beszel-system__state">{system.state}</span>
        </header>
        <dl>
          <div><dt>CPU</dt><dd>{percentage(system.cpuPercent)}</dd></div>
          <div><dt>Memory</dt><dd>{percentage(system.memoryPercent)}</dd></div>
          <div><dt>Disk</dt><dd>{percentage(system.diskPercent)}</dd></div>
          <div><dt>Load</dt><dd>{load(system.loadAverage)}</dd></div>
        </dl>
      </article>)}
    </div>
  </section>;
}
