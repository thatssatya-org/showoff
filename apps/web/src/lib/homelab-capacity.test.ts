import { describe, expect, it } from "vitest";

import { capacityFillStyle, homelabCapacityPrinciples } from "./homelab-capacity";

describe("homelab capacity principles", () => {
  it("renders only bounded illustrative bar fills", () => {
    expect(homelabCapacityPrinciples.map((principle) => capacityFillStyle(principle.fill))).toEqual([
      "--capacity-fill: 58%",
      "--capacity-fill: 52%",
      "--capacity-fill: 43%"
    ]);
  });

  it("rejects invalid fill values instead of producing a style attribute", () => {
    expect(capacityFillStyle(-1)).toBeNull();
    expect(capacityFillStyle(12.5)).toBeNull();
    expect(capacityFillStyle(101)).toBeNull();
  });
});
