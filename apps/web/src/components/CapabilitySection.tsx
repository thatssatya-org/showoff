import { useEffect, useState, type ComponentType } from "react";

import ContributionHeatmap from "./ContributionHeatmap";
import { parseContributionSummary } from "../lib/github-contributions";
import { fetchCapabilities, fetchOptionalCapability, type CapabilityDescriptor } from "../lib/api/capabilities";
import type { components } from "../lib/api/openapi.generated";

type CapabilitySnapshot = Readonly<Required<components["schemas"]["CapabilitySnapshotResponse"]>>;

type CapabilityContentProps = Readonly<{
  content: Readonly<Record<string, string>>;
}>;

type GitHubActivity = Readonly<{ type: string; day: string; repository: string }>;

type ActivityDay = Readonly<{ day: string; activities: readonly GitHubActivity[] }>;

type GitHubRepository = Readonly<{
  repository: string;
  url: string;
  stars: number;
  latestCommitDate: string;
  language?: string;
  latestReleaseTag?: string;
  latestReleaseDate?: string;
  latestReleaseUrl?: string;
}>;

type CapabilityRenderer = Readonly<{
  Component: ComponentType<CapabilityContentProps>;
  isRenderable: (content: Readonly<Record<string, string>>) => boolean;
}>;

function isCapabilitySnapshot(value: CapabilitySnapshot | null): value is CapabilitySnapshot {
  if (typeof value !== "object" || value === null) return false;
  const snapshot = value as Record<string, unknown>;
  return typeof snapshot.capability === "string"
    && typeof snapshot.componentType === "string"
    && typeof snapshot.state === "string"
    && typeof snapshot.title === "string"
    && typeof snapshot.sourceLabel === "string"
    && typeof snapshot.refreshedAt === "string"
    && typeof snapshot.content === "object"
    && snapshot.content !== null
    && Object.values(snapshot.content).every((content) => typeof content === "string");
}

function parseGitHubActivities(value: string | undefined): readonly GitHubActivity[] {
  if (value === undefined) return [];
  try {
    const parsed: unknown = JSON.parse(value);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((event): event is GitHubActivity => typeof event === "object" && event !== null
      && typeof (event as Record<string, unknown>).type === "string"
      && typeof (event as Record<string, unknown>).day === "string"
      && typeof (event as Record<string, unknown>).repository === "string");
  } catch {
    return [];
  }
}

function groupActivitiesByDay(activities: readonly GitHubActivity[]): readonly ActivityDay[] {
  const groups = new Map<string, GitHubActivity[]>();
  activities.forEach((activity) => {
    const sameDay = groups.get(activity.day) ?? [];
    sameDay.push(activity);
    groups.set(activity.day, sameDay);
  });
  return [...groups.entries()].map(([day, groupedActivities]) => ({ day, activities: groupedActivities }));
}

function eventLabel(type: string): string {
  const labels: Readonly<Record<string, string>> = {
    CreateEvent: "created",
    DeleteEvent: "deleted",
    ForkEvent: "forked",
    IssuesEvent: "updated an issue in",
    PullRequestEvent: "opened a pull request in",
    PushEvent: "pushed to",
    ReleaseEvent: "released from",
    WatchEvent: "starred"
  };
  return labels[type] ?? type.replace(/Event$/, "").replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
}

function formatDay(day: string): string {
  const parsed = new Date(`${day}T00:00:00Z`);
  return Number.isNaN(parsed.valueOf())
    ? day
    : new Intl.DateTimeFormat("en", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" }).format(parsed);
}

function formatRefreshedAt(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.valueOf())
    ? "Cache time unavailable"
    : `Updated ${new Intl.DateTimeFormat("en", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(parsed)}`;
}

function parseGitHubRepository(content: Readonly<Record<string, string>>): GitHubRepository | null {
  const stars = Number.parseInt(content.stars ?? "", 10);
  if (content.repository === undefined || content.repository.length === 0 || content.url === undefined || !content.url.startsWith("https://")
    || !Number.isSafeInteger(stars) || stars < 0 || content.latestCommitDate === undefined || Number.isNaN(new Date(content.latestCommitDate).valueOf())) {
    return null;
  }
  return {
    repository: content.repository,
    url: content.url,
    stars,
    latestCommitDate: content.latestCommitDate,
    ...(content.language === undefined ? {} : { language: content.language }),
    ...(content.latestReleaseTag === undefined ? {} : { latestReleaseTag: content.latestReleaseTag }),
    ...(content.latestReleaseDate === undefined ? {} : { latestReleaseDate: content.latestReleaseDate }),
    ...(content.latestReleaseUrl === undefined ? {} : { latestReleaseUrl: content.latestReleaseUrl })
  };
}

function ActivityTimeline({ content }: CapabilityContentProps) {
  const activities = parseGitHubActivities(content.events);
  const activityDays = groupActivitiesByDay(activities);

  if (activityDays.length === 0) return <p className="capability-card__empty">No recent public events in this cache window.</p>;

  return <div className="capability-card__activity" aria-label="Recent GitHub activity">
    {activityDays.map(({ day, activities: activitiesForDay }) => <section className="activity-day" key={day}>
      <time className="activity-day__date" dateTime={day}>{formatDay(day)}</time>
      <ol>
        {activitiesForDay.map((activity, index) => <li key={`${activity.day}-${activity.repository}-${index}`}>
          <span className="activity-event">{eventLabel(activity.type)}</span>
          <strong title={activity.repository}>{activity.repository}</strong>
        </li>)}
      </ol>
    </section>)}
  </div>;
}

function RepositoryGrid({ content }: CapabilityContentProps) {
  const repository = parseGitHubRepository(content);
  if (repository === null) return <p className="capability-card__empty">No approved repository snapshot is available.</p>;

  const release = repository.latestReleaseTag === undefined ? null : repository.latestReleaseUrl === undefined
    ? repository.latestReleaseTag
    : <a href={repository.latestReleaseUrl}>Release {repository.latestReleaseTag}</a>;

  return <div className="capability-card__repository">
    <a className="capability-card__repository-name" href={repository.url}>{repository.repository}</a>
    <dl>
      <div><dt>Stars</dt><dd>{repository.stars}</dd></div>
      {repository.language !== undefined && <div><dt>Language</dt><dd>{repository.language}</dd></div>}
      <div><dt>Latest commit</dt><dd><time dateTime={repository.latestCommitDate}>{formatDay(repository.latestCommitDate.slice(0, 10))}</time></dd></div>
      {release !== null && <div><dt>Latest release</dt><dd>{release}</dd></div>}
    </dl>
  </div>;
}

const CAPABILITY_RENDERERS: Readonly<Record<string, CapabilityRenderer>> = {
  ACTIVITY_TIMELINE: {
    Component: ActivityTimeline,
    isRenderable: () => true
  },
  CONTRIBUTION_HEATMAP: {
    Component: ContributionHeatmap,
    isRenderable: (content) => parseContributionSummary(content) !== null
  },
  REPOSITORY_GRID: {
    Component: RepositoryGrid,
    isRenderable: (content) => parseGitHubRepository(content) !== null
  }
};

function renderableCapabilities(
  capabilities: readonly CapabilityDescriptor[],
  snapshots: ReadonlyMap<string, CapabilitySnapshot>
): readonly Readonly<{ capability: CapabilityDescriptor; snapshot: CapabilitySnapshot; renderer: CapabilityRenderer }>[] {
  return capabilities.flatMap((capability) => {
    const renderer = CAPABILITY_RENDERERS[capability.componentType as string];
    const snapshot = snapshots.get(capability.capability);
    if (renderer === undefined || snapshot === undefined || !renderer.isRenderable(snapshot.content)) return [];
    return [{ capability, snapshot, renderer }];
  });
}

function CapabilityCard({ capability, snapshot, renderer }: Readonly<{
  capability: CapabilityDescriptor;
  snapshot: CapabilitySnapshot;
  renderer: CapabilityRenderer;
}>) {
  const Renderer = renderer.Component;
  const isStale = snapshot.state === "STALE";

  return (
    <article className="capability-card card card-reveal" data-source={capability.sourceLabel.toLowerCase()}>
      <header className="capability-card__header">
        <div>
          <p className="source-meta">Source / {capability.sourceLabel}</p>
          <h3>{capability.title}</h3>
        </div>
        <span className="capability-card__status"><span aria-hidden="true"></span>{isStale ? "cached snapshot" : "cached"}</span>
      </header>
      <Renderer content={snapshot.content} />
      <footer className="capability-card__footer">
        <span>{isStale ? "Last known good snapshot" : "Cached public snapshot"}</span>
        <time dateTime={snapshot.refreshedAt}>{formatRefreshedAt(snapshot.refreshedAt)}</time>
      </footer>
    </article>
  );
}

export default function CapabilitySection() {
  const [capabilities, setCapabilities] = useState<readonly CapabilityDescriptor[]>([]);
  const [snapshots, setSnapshots] = useState<ReadonlyMap<string, CapabilitySnapshot>>(new Map());

  useEffect(() => {
    const controller = new AbortController();
    void fetchCapabilities(controller.signal).then(async (response) => {
      const knownCapabilities = response.filter((capability) => {
        const isKnown = CAPABILITY_RENDERERS[capability.componentType as string] !== undefined;
        if (!isKnown && import.meta.env.DEV) console.warn(`Ignoring unknown public capability component: ${capability.componentType}`);
        return isKnown;
      });
      const resolvedSnapshots = await Promise.all(knownCapabilities.map(async (capability) => [
        capability.capability,
        await fetchOptionalCapability<CapabilitySnapshot>(capability.dataEndpoint, controller.signal).catch(() => null)
      ] as const));
      const availableSnapshots = new Map<string, CapabilitySnapshot>();
      resolvedSnapshots.forEach(([capability, snapshot]) => {
        if (isCapabilitySnapshot(snapshot)) availableSnapshots.set(capability, snapshot);
      });
      setCapabilities(knownCapabilities);
      setSnapshots(availableSnapshots);
    }).catch(() => undefined);
    return () => controller.abort();
  }, []);

  const visibleCapabilities = renderableCapabilities(capabilities, snapshots);
  if (visibleCapabilities.length === 0) return null;

  return (
    <section className="live-sources home-section section-rule" aria-labelledby="live-sources-title">
      <div className="live-sources__heading">
        <div>
          <p className="section-label">Cached sources</p>
          <h2 id="live-sources-title">Recent public signals.</h2>
        </div>
        <p>Owner-connected sources. No visitor tracking.</p>
      </div>
      <div className="live-sources__grid">
        {visibleCapabilities.map(({ capability, snapshot, renderer }) => <CapabilityCard capability={capability} snapshot={snapshot} renderer={renderer} key={capability.capability} />)}
      </div>
    </section>
  );
}
