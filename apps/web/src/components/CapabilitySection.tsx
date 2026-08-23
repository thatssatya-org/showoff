import { useEffect, useState } from "react";

import { fetchCapabilities, fetchOptionalCapability, type CapabilityDescriptor } from "../lib/api/capabilities";

const KNOWN_COMPONENTS = new Set(["MUSIC_CARD", "ACTIVITY_TIMELINE", "REPOSITORY_GRID", "SOCIAL_GRID", "HOMELAB_SUMMARY"]);

function CapabilityCard({ capability }: Readonly<{ capability: CapabilityDescriptor }>) {
  const [content, setContent] = useState<Record<string, unknown> | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    void fetchOptionalCapability<{ content?: Record<string, unknown> }>(capability.dataEndpoint, controller.signal)
      .then((snapshot) => setContent(snapshot?.content ?? null))
      .catch(() => undefined);
    return () => controller.abort();
  }, [capability.dataEndpoint]);

  return (
    <article className="capability-card">
      <p>{capability.sourceLabel}</p>
      <h2>{capability.title}</h2>
      {capability.capability === "GITHUB_ACTIVITY" && content && <GitHubActivity content={content} />}
      {capability.capability !== "GITHUB_ACTIVITY" && <a href={capability.dataEndpoint}>View cached update</a>}
      <small>Last refreshed {new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(capability.refreshedAt))}</small>
    </article>
  );
}

function GitHubActivity({ content }: Readonly<{ content: Record<string, unknown> }>) {
  const events = Array.isArray(content.events) ? content.events.filter(isRecord) : [];
  const contributions = isRecord(content.contributions) ? content.contributions : null;
  const repositories = Array.isArray(content.repositories) ? content.repositories.filter(isRecord) : [];
  return <>
    {typeof contributions?.total === "number" && <p>{contributions.total} contributions in the published calendar.</p>}
    {events.length > 0 && <ul aria-label="Recent public GitHub activity">{events.map((event, index) =>
      <li key={`${String(event.day)}-${String(event.repository)}-${index}`}>{String(event.type)} · {String(event.repository)} · {String(event.day)}</li>)}</ul>}
    {repositories.map((repository) => <section key={String(repository.url)} aria-label={`Repository ${String(repository.name)}`}>
      <h3><a href={String(repository.url)} rel="external">{String(repository.name)}</a></h3>
      {typeof repository.description === "string" && <p>{repository.description}</p>}
      <p>{typeof repository.stars === "number" ? `${repository.stars} stars` : null}{typeof repository.primaryLanguage === "string" ? ` · ${repository.primaryLanguage}` : null}{typeof repository.updatedAt === "string" ? ` · updated ${repository.updatedAt}` : null}</p>
      {Array.isArray(repository.topics) && repository.topics.every((topic) => typeof topic === "string") && <p>{repository.topics.join(" · ")}</p>}
    </section>)}
  </>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

export default function CapabilitySection() {
  const [capabilities, setCapabilities] = useState<readonly CapabilityDescriptor[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    void fetchCapabilities(controller.signal).then((response) => {
      setCapabilities(response.filter((capability) => KNOWN_COMPONENTS.has(capability.componentType)));
    }).catch(() => undefined);
    return () => controller.abort();
  }, []);

  if (capabilities.length === 0) return null;
  return (
    <section aria-labelledby="live-sources-title">
      <p>Cached sources</p>
      <h2 id="live-sources-title">Selected recent signals</h2>
      <div>{capabilities.map((capability) => <CapabilityCard capability={capability} key={capability.capability} />)}</div>
    </section>
  );
}
