import { describe, expect, it } from "vitest";

import { contributionLevel, formatContributionDay, parseContributionSummary } from "./github-contributions";

describe("GitHub contribution summary", () => {
  it("accepts only the anonymous day/count projection", () => {
    expect(parseContributionSummary({
      totalContributions: "3",
      includesPrivateContributions: "true",
      contributionDays: JSON.stringify([
        { date: "2026-08-22", count: 0 },
        { date: "2026-08-23", count: 3 }
      ])
    })).toEqual({
      totalContributions: 3,
      includesPrivateContributions: true,
      days: [
        { date: "2026-08-22", count: 0 },
        { date: "2026-08-23", count: 3 }
      ]
    });
  });

  it("rejects malformed calendar data", () => {
    expect(parseContributionSummary({
      totalContributions: "2",
      includesPrivateContributions: "false",
      contributionDays: '[{"date":"2026-08-23","count":-1}]'
    })).toBeNull();
  });

  it("uses a readable day label and bounded visual levels", () => {
    expect(formatContributionDay("2026-08-23", 1)).toBe("August 23, 2026: 1 contribution");
    expect(contributionLevel(0, 9)).toBe(0);
    expect(contributionLevel(9, 9)).toBe(4);
  });
});
