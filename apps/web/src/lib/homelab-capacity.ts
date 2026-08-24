export type CapacityPrinciple = Readonly<{
  label: "CPU" | "MEM" | "DISK I/O";
  posture: string;
  explanation: string;
  fill: number;
}>;

export const homelabCapacityPrinciples = [
  {
    label: "CPU",
    posture: "leave room for bursts",
    explanation: "Keep routine work comfortably below the point where interactive tasks compete.",
    fill: 58
  },
  {
    label: "MEM",
    posture: "prefer recoverable pressure",
    explanation: "Treat memory headroom and restart behaviour as design constraints, not afterthoughts.",
    fill: 52
  },
  {
    label: "DISK I/O",
    posture: "separate noisy work",
    explanation: "Plan for contention before maintenance jobs and data movement make it visible.",
    fill: 43
  }
] as const satisfies readonly CapacityPrinciple[];

export function capacityFillStyle(fill: number): string | null {
  if (!Number.isInteger(fill) || fill < 0 || fill > 100) {
    return null;
  }

  return `--capacity-fill: ${fill}%`;
}
