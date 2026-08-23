import { useEffect, useState } from "react";

import { fetchCapabilities, fetchOptionalCapability, type CapabilityDescriptor } from "../lib/api/capabilities";

const KNOWN_COMPONENTS = new Set(["MUSIC_CARD", "ACTIVITY_TIMELINE", "REPOSITORY_GRID", "SOCIAL_GRID", "HOMELAB_SUMMARY"]);

type CapabilitySnapshot = Readonly<{
  capability: string;
  componentType: string;
  state: string;
  title: string;
  sourceLabel: string;
  refreshedAt: string;
  content: Readonly<Record<string, string>>;
}>;

type GitHubActivity = Readonly<{ type: string; day: string; repository: string }>;

type ActivityDay = Readonly<{ day: string; activities: readonly GitHubActivity[] }>;

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

function CapabilityCard({ capability, snapshot }: Readonly<{
  capability: CapabilityDescriptor;
  snapshot: CapabilitySnapshot | null;
}>) {
  const activities = capability.componentType === "ACTIVITY_TIMELINE"
    ? parseGitHubActivities(snapshot?.content.events)
    : [];
  const activityDays = groupActivitiesByDay(activities);

  return (
    <article className="capability-card card card-reveal" data-source={capability.sourceLabel.toLowerCase()}>
      <header className="capability-card__header">
        <div>
          <p className="source-meta">Source / {capability.sourceLabel}</p>
          <h3>{capability.title}</h3>
        </div>
        <span className="capability-card__status"><span aria-hidden="true"></span> cached</span>
      </header>
      {activityDays.length > 0 && <div className="capability-card__activity" aria-label="Recent GitHub activity">
        {activityDays.map(({ day, activities: activitiesForDay }) => <section className="activity-day" key={day}>
          <time className="activity-day__date" dateTime={day}>{formatDay(day)}</time>
          <ol>
            {activitiesForDay.map((activity, index) => <li key={`${activity.day}-${activity.repository}-${index}`}>
              <span className="activity-event">{eventLabel(activity.type)}</span>
              <strong title={activity.repository}>{activity.repository}</strong>
            </li>)}
          </ol>
        </section>)}
      </div>}
      {activities.length === 0 && <p className="capability-card__empty">No recent public events in this cache window.</p>}
      <footer className="capability-card__footer">
        <span>{activities.length > 0 ? `${activities.length} public event${activities.length === 1 ? "" : "s"}` : "Snapshot unavailable"}</span>
        <time dateTime={capability.refreshedAt}>{formatRefreshedAt(capability.refreshedAt)}</time>
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
      const visibleCapabilities = response.filter((capability) => KNOWN_COMPONENTS.has(capability.componentType));
      setCapabilities(visibleCapabilities);
      const resolvedSnapshots = await Promise.all(visibleCapabilities.map(async (capability) => [
        capability.capability,
        await fetchOptionalCapability<CapabilitySnapshot>(capability.dataEndpoint, controller.signal)
      ] as const));
      const availableSnapshots = new Map<string, CapabilitySnapshot>();
      resolvedSnapshots.forEach(([capability, snapshot]) => {
        if (snapshot !== null) availableSnapshots.set(capability, snapshot);
      });
      setSnapshots(availableSnapshots);
    }).catch(() => undefined);
    return () => controller.abort();
  }, []);

  if (capabilities.length === 0) return null;
  return (
    <section className="live-sources home-section section-rule" aria-labelledby="live-sources-title">
      <div className="live-sources__heading">
        <div>
          <p className="section-label">Cached sources</p>
          <h2 id="live-sources-title">Recent public signals.</h2>
        </div>
        <p>Owner-connected sources. No visitor tracking.</p>
      </div>
      <div className="live-sources__grid">{capabilities.map((capability) => <CapabilityCard capability={capability} snapshot={snapshots.get(capability.capability) ?? null} key={capability.capability} />)}</div>
    </section>
  );
}
