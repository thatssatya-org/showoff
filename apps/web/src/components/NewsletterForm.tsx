import { useId, useState } from "react";

type FormState = "idle" | "sending" | "success" | "rate_limited" | "network_error";

interface Props {
  consentVersion: string;
  consentText: string;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/u;

export default function NewsletterForm({ consentVersion, consentText }: Props) {
  const emailId = useId();
  const errorId = useId();
  const statusId = useId();
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string>();
  const [state, setState] = useState<FormState>("idle");

  const submit = async (event: { preventDefault: () => void }) => {
    event.preventDefault();
    const normalizedEmail = email.trim();
    if (!EMAIL_PATTERN.test(normalizedEmail)) {
      setError("Enter a valid email address.");
      return;
    }

    setError(undefined);
    setState("sending");
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 8_000);

    try {
      const response = await fetch("/api/v1/newsletter/subscriptions", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({
          email: normalizedEmail,
          consentVersion,
          source: "portfolio",
          website: ""
        }),
        signal: controller.signal
      });
      if (response.ok) {
        setState("success");
        setEmail("");
      } else if (response.status === 429) {
        setState("rate_limited");
      } else if (response.status === 400) {
        setState("idle");
        setError("Check the email address and try again.");
      } else {
        setState("network_error");
      }
    } catch {
      setState("network_error");
    } finally {
      window.clearTimeout(timeout);
    }
  };

  const status = {
    idle: "",
    sending: "Sending subscription request…",
    success: "Check your inbox to confirm the subscription.",
    rate_limited: "Too many requests. Try again shortly.",
    network_error: "The request did not reach the service. Try again."
  }[state];

  return (
    <form className="newsletter-form" onSubmit={submit} noValidate>
      <label htmlFor={emailId}>Email address</label>
      <div className="newsletter-form__row">
        <input
          id={emailId}
          name="email"
          type="email"
          autoComplete="email"
          inputMode="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          aria-describedby={error ? errorId : statusId}
          aria-invalid={Boolean(error)}
          required
        />
        <button type="submit" disabled={state === "sending"}>{state === "sending" ? "Sending" : "Subscribe"}</button>
      </div>
      <label className="honeypot" htmlFor={`${emailId}-website`}>Leave this blank<input id={`${emailId}-website`} name="website" tabIndex={-1} autoComplete="off" /></label>
      <p className="consent">{consentText}</p>
      {error ? <p id={errorId} className="error" role="alert">{error}</p> : null}
      <p id={statusId} className="status" aria-live="polite">{status}</p>
    </form>
  );
}
