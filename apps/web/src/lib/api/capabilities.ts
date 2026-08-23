import type { components } from "./openapi.generated";

export type CapabilityDescriptor = Readonly<Required<
  components["schemas"]["CapabilityDescriptorResponse"]
>>;

const REQUEST_TIMEOUT_MS = 6_000;

export async function fetchCapabilities(signal?: AbortSignal): Promise<readonly CapabilityDescriptor[]> {
  const controller = new AbortController();
  const timeout = globalThis.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const abort = () => controller.abort();
  signal?.addEventListener("abort", abort, { once: true });

  try {
    const response = await fetch("/api/v1/capabilities", {
      headers: { Accept: "application/json" },
      cache: "no-cache",
      signal: controller.signal
    });
    if (response.status === 204) return [];
    if (!response.ok) return [];
    const body: unknown = await response.json();
    return Array.isArray(body) ? body.filter(isCapabilityDescriptor) : [];
  } finally {
    globalThis.clearTimeout(timeout);
    signal?.removeEventListener("abort", abort);
  }
}

function isCapabilityDescriptor(value: unknown): value is CapabilityDescriptor {
  if (typeof value !== "object" || value === null) return false;
  const descriptor = value as Record<string, unknown>;
  return typeof descriptor.capability === "string"
    && typeof descriptor.componentType === "string"
    && typeof descriptor.dataEndpoint === "string"
    && typeof descriptor.title === "string"
    && typeof descriptor.sourceLabel === "string"
    && typeof descriptor.refreshedAt === "string";
}

export async function fetchOptionalCapability<T>(endpoint: string, signal?: AbortSignal): Promise<T | null> {
  const response = await fetch(endpoint, { headers: { Accept: "application/json" }, signal });
  if (response.status === 204) return null;
  if (!response.ok) throw new Error(`Public capability request failed with ${response.status}`);
  return response.json() as Promise<T>;
}
