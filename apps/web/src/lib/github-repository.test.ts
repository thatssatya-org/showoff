import { describe, expect, it } from "vitest";

import { matchesPublicRepository, parseGitHubRepository } from "./github-repository";

const validContent = {
  repository: "thatssatya-org/easyfintrack",
  url: "https://github.com/thatssatya-org/easyfintrack",
  stars: "42",
  latestCommitDate: "2026-08-23T10:00:00Z",
  language: "TypeScript",
  latestReleaseTag: "v1.2.0",
  latestReleaseDate: "2026-08-20T10:00:00Z",
  latestReleaseUrl: "https://github.com/thatssatya-org/easyfintrack/releases/tag/v1.2.0"
};

describe("GitHub repository projection", () => {
  it("parses the bounded public repository fields", () => {
    expect(parseGitHubRepository(validContent)).toEqual({ ...validContent, stars: 42 });
  });

  it.each([
    [{ ...validContent, stars: "1 star" }],
    [{ ...validContent, stars: "-1" }],
    [{ ...validContent, url: "http://github.com/thatssatya-org/easyfintrack" }],
    [{ ...validContent, url: "https://example.test/thatssatya-org/easyfintrack" }],
    [{ ...validContent, url: "https://github.com/another-owner/easyfintrack" }],
    [{ ...validContent, latestCommitDate: "not-a-date" }],
    [{ ...validContent, latestReleaseUrl: "javascript:alert(1)" }],
    [{ ...validContent, latestReleaseUrl: "https://example.test/thatssatya-org/easyfintrack/releases/tag/v1.2.0" }],
    [{ ...validContent, latestReleaseUrl: "https://github.com/another-owner/easyfintrack/releases/tag/v1.2.0" }],
    [{ ...validContent, latestReleaseUrl: "https://github.com/thatssatya-org/easyfintrack/issues/1" }],
    [{ ...validContent, latestReleaseUrl: "https://github.com/thatssatya-org/easyfintrack/releases/tag/v1.2.0?download=1" }]
  ])("rejects an invalid projection", (content) => {
    expect(parseGitHubRepository(content)).toBeNull();
  });

  it("matches only the exact curated repository identifier", () => {
    const repository = parseGitHubRepository(validContent);
    expect(repository).not.toBeNull();
    if (repository === null) return;

    expect(matchesPublicRepository(repository, "thatssatya-org/easyfintrack")).toBe(true);
    expect(matchesPublicRepository(repository, "thatssatya-org/EasyFintrack")).toBe(false);
    expect(matchesPublicRepository(repository, "another-owner/easyfintrack")).toBe(false);
  });
});
