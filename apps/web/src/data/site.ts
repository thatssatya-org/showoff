export type SocialLink = Readonly<{
  label: string;
  href: string;
}>;

export type UsesEntry = Readonly<{
  category: string;
  items: readonly string[];
}>;

export type RightNowItem = Readonly<{
  title: string;
  description: string;
  href: string;
  source: "curated" | "music" | "social";
  presentation?: "link" | "music-link" | "photo-carousel";
  photos?: readonly Readonly<{
    src: string;
    srcSet: string;
    width: number;
    height: number;
    alt: string;
  }>[];
}>;

export const siteIdentity = {
  name: "Satyajit Roy",
  identityLine: "Backend systems. Quietly engineered to survive the interesting failures.",
  themes: ["Code", "Music", "Bikes", "Cats"],
  thesis:
    "Bengaluru-based fintech backend engineer. I build low-latency, recoverable systems and keep the public surface deliberately small.",
  location: "Bengaluru, India",
  contactLabel: "DM on Instagram",
  contactHref: "https://www.instagram.com/thatssatya",
  githubProfile: "https://github.com/thatssatya",
  newsletterEnabled: false,
  socialLinks: [
    { label: "Spotify", href: "https://open.spotify.com/user/ubhku2hccfwxobdfjzbh7dk20" },
    { label: "X", href: "https://x.com/thatssatya" },
    { label: "Instagram", href: "https://www.instagram.com/thatssatya" },
    { label: "LinkedIn", href: "https://www.linkedin.com/in/thatssatya" },
    { label: "GitHub", href: "https://github.com/thatssatya" },
    { label: "Buy me a coffee", href: "https://buymeacoffee.com/thatssatya" },
    { label: "YouTube", href: "https://www.youtube.com/@TheMotoDirector" }
  ] satisfies readonly SocialLink[]
} as const;

export const mustListen = {
  title: "Shaukeens",
  description:
    "A public playlist, selected by Satyajit. Open it directly in Spotify.",
  href: "https://open.spotify.com/playlist/6YwNbPTVoemt05mtYiNTNN"
} as const;

export const rightNowItems = [
  {
    title: "Chat with AI!",
    description: "An experimental AI conversation surface.",
    href: "https://chad-ai.vercel.app/",
    source: "curated"
  },
  {
    title: "I'm listening to...",
    description: "On Repeat — a rolling trace of the songs that keep surviving the skip button.",
    href: "https://open.spotify.com/playlist/37i9dQZF1Epg41WGRDMFWq?si=-0ZQIBuwTa-DEo5TMFFnvQ&utm_source=copy-link&pi=eZPwWQnCRaWdt",
    source: "music",
    presentation: "music-link"
  },
  {
    title: "Aero India '23",
    description: "A Bengaluru flightline field note: metal, lift, and machines built to negotiate with gravity.",
    href: "https://photos.app.goo.gl/GPD5L6yLHqgWT7Wf9",
    source: "social",
    presentation: "photo-carousel",
    photos: [
      {
        src: "/media/aero-india-23/01-960.webp",
        srcSet: "/media/aero-india-23/01-480.webp 480w, /media/aero-india-23/01-960.webp 960w",
        width: 1200,
        height: 900,
        alt: "Aero India exhibition entrance signage"
      },
      {
        src: "/media/aero-india-23/02-960.webp",
        srcSet: "/media/aero-india-23/02-480.webp 270w, /media/aero-india-23/02-960.webp 540w",
        width: 900,
        height: 1600,
        alt: "Aero India venue approach with gardens and flags"
      },
      {
        src: "/media/aero-india-23/03-960.webp",
        srcSet: "/media/aero-india-23/03-480.webp 480w, /media/aero-india-23/03-960.webp 960w",
        width: 1200,
        height: 900,
        alt: "Aircraft exhibit at Aero India"
      },
      {
        src: "/media/aero-india-23/04-960.webp",
        srcSet: "/media/aero-india-23/04-480.webp 480w, /media/aero-india-23/04-960.webp 960w",
        width: 1200,
        height: 900,
        alt: "Aviation display at Aero India"
      },
      {
        src: "/media/aero-india-23/05-960.webp",
        srcSet: "/media/aero-india-23/05-480.webp 480w, /media/aero-india-23/05-960.webp 960w",
        width: 1200,
        height: 900,
        alt: "Military equipment exhibit at Aero India"
      }
    ]
  }
] as const satisfies readonly RightNowItem[];

export const sourceCategories = ["curated", "github", "music", "social", "systems"] as const;

export const newsletterConsent = {
  version: "portfolio-newsletter-2026-08-23",
  text:
    "I agree to receive occasional emails from Satyajit Roy at this address. I can unsubscribe at any time. My address is used only to operate this newsletter, as described in the Privacy page."
} as const;

export const siteUrl = "https://thatssatya.github.io";

export const publicFocus = [
  "Backend systems where correctness, recovery, and latency are treated as product work.",
  "Small self-hosted services behind private networking, with public surfaces kept intentionally boring.",
  "A curated public record: useful signals, no surveillance theatre."
] as const;

export const homelabStory = {
  title: "The quiet machine room",
  body:
    "A self-hosted proving ground where automation, media, AI, and file systems are built to recover cleanly when the network gets interesting. The machinery stays private; the engineering discipline is the part worth publishing.",
  categories: ["media", "AI gateway", "photo management", "file tooling", "automation", "monitoring", "remote access"] as const,
  principles: [
    "Private overlay access before public exposure.",
    "Container boundaries, least privilege, and recoverable backups.",
    "Coarse, delayed public summaries only when explicitly approved."
  ] as const
} as const;

export const nowNote = {
  title: "Current field note",
  body:
    "The public record is deliberately sparse while content and vendor permissions are reviewed. Follow an approved link or return later for a selected update.",
  updatedLabel: "Owner-reviewed content"
} as const;

export const usesEntries = [
  {
    category: "Engineering",
    items: ["Java", "Spring Boot", "MongoDB", "Temporal", "Docker Compose"]
  },
  {
    category: "Operating principles",
    items: ["Static-first public surfaces", "Tailscale-style private access", "Bounded data and explicit ownership"]
  },
  {
    category: "Publishing rule",
    items: ["Only owner-approved tools, links, and capabilities belong here."]
  }
] as const satisfies readonly UsesEntry[];
