import { useState } from "react";

type SaveState = "idle" | "saving" | "saved" | "failed";

const TOKEN_ENDPOINT = "/operator/github/token";

export default function GitHubConnectionSetup() {
  const [token, setToken] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [saveState, setSaveState] = useState<SaveState>("idle");

  async function saveToken(event: { preventDefault(): void }) {
    event.preventDefault();
    if (!confirmed || token.trim().length === 0) return;

    const submittedToken = token;
    setToken("");
    setSaveState("saving");

    try {
      const response = await fetch(TOKEN_ENDPOINT, {
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
    <section className="operator-connection card" aria-labelledby="github-connection-title">
      <div className="operator-connection__heading">
        <p className="section-label">Connection / GitHub</p>
        <h1 id="github-connection-title">Store the GitHub sync credential.</h1>
        <p>
          This operator route is available only on the Tailnet. The token is used only for cached GitHub sync;
          visitors never trigger a GitHub request. This browser does not retain the token after this attempt.
        </p>
      </div>

      <form className="operator-connection__form" onSubmit={saveToken}>
        <label htmlFor="github-token">Fine-grained personal access token</label>
        <input
          id="github-token"
          name="github-token"
          type="password"
          autoComplete="off"
          spellCheck={false}
          value={token}
          onChange={(event) => {
            setToken(event.target.value);
            if (saveState !== "idle") setSaveState("idle");
          }}
          aria-describedby="github-token-notice github-token-status"
          required
        />
        <p id="github-token-notice" className="operator-connection__notice">
          Do not paste a token into the public site or a chat. It is sent once to the private write-only endpoint.
        </p>
        <label className="operator-connection__confirm">
          <input
            type="checkbox"
            checked={confirmed}
            onChange={(event) => setConfirmed(event.target.checked)}
          />
          <span>I understand this replaces the stored GitHub sync token.</span>
        </label>
        <button type="submit" disabled={!confirmed || token.trim().length === 0 || saveState === "saving"}>
          {saveState === "saving" ? "Saving token…" : "Save GitHub token"}
        </button>
        <p id="github-token-status" className={saveState === "failed" ? "operator-connection__error" : "operator-connection__status"} role="status" aria-live="polite">
          {saveState === "saved" && "GitHub token saved. Cached sync can use it on its next scheduled run."}
          {saveState === "failed" && "The token was not saved. Confirm that this Tailnet-only operator route is available, then try again."}
        </p>
      </form>

      <aside className="operator-connection__future" aria-label="Future connection methods">
        <p className="section-label">Reserved connection method</p>
        <p>OAuth connections will use this same operator setup surface when a provider requires them. OAuth is not enabled here.</p>
      </aside>
    </section>
  );
}
