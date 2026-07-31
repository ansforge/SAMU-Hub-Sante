import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function buildGithubSchemaUrl(schemaName: string, ref: string): string {
  return `https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/${ref}/src/main/resources/json-schema/${schemaName}`;
}
