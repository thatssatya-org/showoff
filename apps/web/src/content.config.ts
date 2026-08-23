import { defineCollection } from "astro:content";
import { glob } from "astro/loaders";
import { z } from "astro/zod";

export const projectSchema = z.object({
  title: z.string().min(1),
  summary: z.string().min(1),
  projectSlug: z.string().regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
  visible: z.literal(true),
  order: z.number().int().nonnegative(),
  externalUrl: z.url().optional(),
  sourceLabel: z.string().min(1),
  imageAlt: z.string().min(1).optional(),
  expiresAt: z.coerce.date().optional(),
  stack: z.array(z.string().min(1)).default([])
});

const projects = defineCollection({
  loader: glob({ base: "./src/content/projects", pattern: "**/*.md" }),
  schema: projectSchema
});

export const collections = { projects };
