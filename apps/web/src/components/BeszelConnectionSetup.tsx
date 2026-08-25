import { useState } from "react";

type SaveState = "idle" | "saving" | "saved" | "failed";

const PAIR_ENDPOINT = "/operator/beszel/pair";

export default function BeszelConnectionSetup() {
  const [token, setToken] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [saveState, setSaveState] = useState<SaveState>("idle");

  async function pair(event: { preventDefault(): void }) {
    event.preventDefault();
    if (!confirmed || token.trim().length === 0) return;

    const submittedToken = token;
    setToken("");
    setSaveState("saving");
    try {
      const response = await fetch(PAIR_ENDPOINT, {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ token: submittedToken })
      });
      setSaveState(response.status === 204 ? "saved" : "failed");
    } catch {
      setSaveState("failed");
    }
  }

  return (
    <section className="operator-connection card" aria-labelledby="beszel-connection-title">
      <div className="operator-connection__heading">
        <p className="section-label">Connection / Beszel</p>
        <h1 id="beszel-connection-title">Pair the metrics source.</h1>
        <p>
          This Tailnet-only route submits the Beszel access token once. Showoff encrypts it server-side;
          this browser neither retains it nor connects to Beszel directly.
        </p>
      </div>

      <form className="operator-connection__form" onSubmit={pair}>
        <label htmlFor="beszel-token">Beszel access token</label>
        <input
          id="beszel-token"
          name="beszel-token"
          type="password"
          autoComplete="off"
          spellCheck={false}
          value={token}
          onChange={(event) => {
            setToken(event.target.value);
            if (saveState !== "idle") setSaveState("idle");
          }}
          aria-describedby="beszel-token-notice beszel-token-status"
          required
        />
        <p id="beszel-token-notice" className="operator-connection__notice">
          The Beszel Tailnet origin is backend configuration. Do not paste an origin, dashboard link, or token into the public site or chat.
        </p>
        <label className="operator-connection__confirm">
          <input type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} />
          <span>I understand this replaces the stored Beszel access token.</span>
        </label>
        <button type="submit" disabled={!confirmed || token.trim().length === 0 || saveState === "saving"}>
          {saveState === "saving" ? "Pairing source…" : "Pair Beszel"}
        </button>
        <p id="beszel-token-status" className={saveState === "failed" ? "operator-connection__error" : "operator-connection__status"} role="status" aria-live="polite">
          {saveState === "saved" && <>Beszel paired. <a href="/operator/beszel/dashboard/">Open the metric dashboard</a>.</>}
          {saveState === "failed" && "The token was not saved. Confirm the Tailnet-only route is available, then try again."}
        </p>
      </form>
    </section>
  );
}
