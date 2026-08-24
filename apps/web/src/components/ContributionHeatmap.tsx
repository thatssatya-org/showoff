import { contributionLevel, formatContributionDay, parseContributionSummary } from "../lib/github-contributions";

type Props = Readonly<{
  content: Readonly<Record<string, string>>;
}>;

export default function ContributionHeatmap({ content }: Props) {
  const summary = parseContributionSummary(content);
  if (summary === null) return null;

  const highestCount = Math.max(...summary.days.map((day) => day.count), 0);

  return (
    <div className="contribution-heatmap">
      <div className="contribution-heatmap__summary">
        <strong>{summary.totalContributions.toLocaleString("en")}</strong>
        <span>contributions in the last year</span>
      </div>
      {summary.includesPrivateContributions && <p className="contribution-heatmap__privacy">
        Includes anonymous private contributions. No private project or event details are shown.
      </p>}
      <div className="contribution-heatmap__calendar" aria-label="GitHub contributions by calendar day">
        {summary.days.map((day) => <span
          aria-label={formatContributionDay(day.date, day.count)}
          className={`contribution-heatmap__day contribution-heatmap__day--${contributionLevel(day.count, highestCount)}`}
          key={day.date}
          role="img"
          title={formatContributionDay(day.date, day.count)}
        />)}
      </div>
      <div className="contribution-heatmap__legend" aria-label="Contribution count intensity legend">
        <span>Less</span>
        {[0, 1, 2, 3, 4].map((level) => <span className={`contribution-heatmap__day contribution-heatmap__day--${level}`} key={level} aria-hidden="true" />)}
        <span>More</span>
      </div>
    </div>
  );
}
