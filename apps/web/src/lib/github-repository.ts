import { fetchCapabilities, fetchOptionalCapability } from "./api/capabilities";
import type { components } from "./api/openapi.generated";

type CapabilitySnapshot = Readonly<Required<components["schemas"]["CapabilitySnapshotResponse"]>>;

export type GitHubRepository = Readonly<{
  repository: string;
  url: string;
  stars: number;
  latestCommitDate: string;
  language?: string;
  latestReleaseTag?: string;
  latestReleaseDate?: string;
  latestReleaseUrl?: string;
}>;

export type RepositoryProvenance = Readonly<{
  repository: GitHubRepository;
  sourceLabel: string;
  refreshedAt: string;
  stale: boolean;
}>;

export function parseGitHubRepository(content: Readonly<Record<string, string>>): GitHubRepository | null {
  const stars = Number.parseInt(content.stars ?? "", 10);
  if (content.repository === undefined || !isRepositoryIdentifier(content.repository)
    || content.url === undefined || !isRepositoryUrl(content.url, content.repository)
    || !/^\d+$/.test(content.stars ?? "") || !Number.isSafeInteger(stars) || stars < 0
    || content.latestCommitDate === undefined || !isDate(content.latestCommitDate)
    || (content.latestReleaseDate !== undefined && !isDate(content.latestReleaseDate))
    || (content.latestReleaseUrl !== undefined && !isReleaseUrl(content.latestReleaseUrl, content.repository))) return null;
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

export function matchesPublicRepository(repository: GitHubRepository, publicRepository: string): boolean {
  return repository.repository === publicRepository;
}

function isDate(value: string): boolean {
  return !Number.isNaN(new Date(value).valueOf());
}

function isRepositoryIdentifier(value: string): boolean {
  return /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(value);
}

function parseSafeGitHubUrl(value: string): URL | null {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname === "github.com" && url.port === ""
      && url.username === "" && url.password === "" && url.search === "" && url.hash === "" ? url : null;
  } catch {
    return null;
  }
}

function isRepositoryUrl(value: string, repository: string): boolean {
  const url = parseSafeGitHubUrl(value);
  return url !== null && (url.pathname === `/${repository}` || url.pathname === `/${repository}/`);
}

function isReleaseUrl(value: string, repository: string): boolean {
  const url = parseSafeGitHubUrl(value);
  const releasePrefix = `/${repository}/releases/tag/`;
  return url !== null && url.pathname.startsWith(releasePrefix) && url.pathname.length > releasePrefix.length;
}

function isRepositorySnapshot(value: unknown): value is CapabilitySnapshot {
  if (typeof value !== "object" || value === null) return false;
  const snapshot = value as Record<string, unknown>;
  return snapshot.componentType === "REPOSITORY_GRID"
    && typeof snapshot.capability === "string" && typeof snapshot.state === "string"
    && typeof snapshot.sourceLabel === "string" && typeof snapshot.refreshedAt === "string"
    && isDate(snapshot.refreshedAt) && typeof snapshot.content === "object" && snapshot.content !== null
    && Object.values(snapshot.content).every((item) => typeof item === "string");
}

let repositoryProvenanceRequest: Promise<RepositoryProvenance | null> | undefined;

export function fetchRepositoryProvenance(): Promise<RepositoryProvenance | null> {
  repositoryProvenanceRequest ??= resolveRepositoryProvenance();
  return repositoryProvenanceRequest;
}

async function resolveRepositoryProvenance(): Promise<RepositoryProvenance | null> {
  const descriptors = await fetchCapabilities();
  const descriptor = descriptors.find(({ componentType }) => componentType === "REPOSITORY_GRID");
  if (descriptor === undefined) return null;
  const snapshot = await fetchOptionalCapability<unknown>(descriptor.dataEndpoint).catch(() => null);
  if (!isRepositorySnapshot(snapshot) || snapshot.capability !== descriptor.capability) return null;
  const repository = parseGitHubRepository(snapshot.content);
  return repository === null ? null : {
    repository, sourceLabel: descriptor.sourceLabel, refreshedAt: snapshot.refreshedAt, stale: snapshot.state === "STALE"
  };
}
