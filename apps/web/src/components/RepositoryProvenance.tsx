import { useEffect, useState } from "react";

import { fetchRepositoryProvenance, matchesPublicRepository, type RepositoryProvenance as Provenance } from "../lib/github-repository";

type Props = Readonly<{ publicRepository: string }>;

function formatRefreshedAt(value: string): string {
  return new Intl.DateTimeFormat("en", {
    month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit", timeZone: "UTC"
  }).format(new Date(value));
}

export default function RepositoryProvenance({ publicRepository }: Props) {
  const [provenance, setProvenance] = useState<Provenance | null>(null);

  useEffect(() => {
    let active = true;
    void fetchRepositoryProvenance().then((result) => {
      if (active && result !== null && matchesPublicRepository(result.repository, publicRepository)) setProvenance(result);
    }).catch(() => undefined);
    return () => { active = false; };
  }, [publicRepository]);

  if (provenance === null) return null;
  const { repository } = provenance;

  return <div className="project-card__provenance" aria-label={`Public repository metadata for ${publicRepository}`}>
    <a className="project-card__source" href={repository.url} target="_blank" rel="noopener noreferrer" aria-label={`${provenance.sourceLabel} source ${repository.repository} (opens in a new tab)`}>
      {provenance.sourceLabel} source / {repository.repository} ↗
    </a>
    <dl>
      {repository.language !== undefined && <div><dt>Language</dt><dd>{repository.language}</dd></div>}
      <div><dt>Stars</dt><dd>{repository.stars.toLocaleString("en")}</dd></div>
      {repository.latestReleaseTag !== undefined && <div><dt>Release</dt><dd>
        {repository.latestReleaseUrl === undefined
          ? repository.latestReleaseTag
          : <a href={repository.latestReleaseUrl} target="_blank" rel="noopener noreferrer" aria-label={`Release ${repository.latestReleaseTag} (opens in a new tab)`}>{repository.latestReleaseTag} ↗</a>}
      </dd></div>}
      <div><dt>Cache</dt><dd><time dateTime={provenance.refreshedAt}>{provenance.stale ? "Last known good · " : "Refreshed · "}{formatRefreshedAt(provenance.refreshedAt)} UTC</time></dd></div>
    </dl>
  </div>;
}
