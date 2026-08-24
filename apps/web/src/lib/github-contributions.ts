export type ContributionDay = Readonly<{
  date: string;
  count: number;
}>;

export type ContributionSummary = Readonly<{
  days: readonly ContributionDay[];
  includesPrivateContributions: boolean;
  totalContributions: number;
}>;

const ISO_DAY = /^\d{4}-\d{2}-\d{2}$/u;

function isContributionDay(value: unknown): value is ContributionDay {
  if (typeof value !== "object" || value === null) return false;
  const day = value as Record<string, unknown>;
  return typeof day.date === "string"
    && ISO_DAY.test(day.date)
    && typeof day.count === "number"
    && Number.isInteger(day.count)
    && day.count >= 0;
}

function parseContributionDays(value: string | undefined): readonly ContributionDay[] | null {
  if (value === undefined) return null;

  try {
    const parsed: unknown = JSON.parse(value);
    if (!Array.isArray(parsed) || !parsed.every(isContributionDay)) return null;

    return [...new Map(parsed.map((day) => [day.date, day])).values()].sort((left, right) => left.date.localeCompare(right.date));
  } catch {
    return null;
  }
}

export function parseContributionSummary(content: Readonly<Record<string, string>>): ContributionSummary | null {
  const totalContributions = Number(content.totalContributions);
  const days = parseContributionDays(content.contributionDays);
  if (!Number.isSafeInteger(totalContributions) || totalContributions < 0 || days === null) return null;

  return {
    days,
    totalContributions,
    includesPrivateContributions: content.includesPrivateContributions === "true"
  };
}

export function contributionLevel(count: number, highestCount: number): 0 | 1 | 2 | 3 | 4 {
  if (count === 0 || highestCount === 0) return 0;
  return Math.min(4, Math.ceil((count / highestCount) * 4)) as 1 | 2 | 3 | 4;
}

export function formatContributionDay(date: string, count: number): string {
  const parsed = new Date(`${date}T00:00:00Z`);
  const label = Number.isNaN(parsed.valueOf())
    ? date
    : new Intl.DateTimeFormat("en", { day: "numeric", month: "long", year: "numeric", timeZone: "UTC" }).format(parsed);
  return `${label}: ${count} contribution${count === 1 ? "" : "s"}`;
}
