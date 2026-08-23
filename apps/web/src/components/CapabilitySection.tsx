import { useEffect, useState } from "react";

import { fetchCapabilities, type CapabilityDescriptor } from "../lib/api/capabilities";

const KNOWN_COMPONENTS = new Set(["MUSIC_CARD", "ACTIVITY_TIMELINE", "REPOSITORY_GRID", "SOCIAL_GRID", "HOMELAB_SUMMARY"]);

function CapabilityCard({ capability }: Readonly<{ capability: CapabilityDescriptor }>) {
  return (
    <article className="capability-card">
      <p>{capability.sourceLabel}</p>
      <h2>{capability.title}</h2>
      <a href={capability.dataEndpoint}>View cached update</a>
      <small>Last refreshed {new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(capability.refreshedAt))}</small>
    </article>
  );
}

export default function CapabilitySection() {
  const [capabilities, setCapabilities] = useState<readonly CapabilityDescriptor[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    void fetchCapabilities(controller.signal).then((response) => {
      setCapabilities(response.filter((capability) => KNOWN_COMPONENTS.has(capability.componentType)));
    }).catch(() => undefined);
    return () => controller.abort();
  }, []);

  if (capabilities.length === 0) return null;
  return (
    <section aria-labelledby="live-sources-title">
      <p>Cached sources</p>
      <h2 id="live-sources-title">Selected recent signals</h2>
      <div>{capabilities.map((capability) => <CapabilityCard capability={capability} key={capability.capability} />)}</div>
    </section>
  );
}
