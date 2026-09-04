import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { repo, schemaPath } from "@/config";
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

function buildGithubRawUrl(ref: string, path: string): string {
  return `https://raw.githubusercontent.com/${repo}/${ref}/${path}`;
}

export function buildGithubSchemaUrl(schemaName: string, ref: string): string {
  return buildGithubRawUrl(ref, `${schemaPath}/${schemaName}`);
}

export function buildNomenclatureUrl(
  nomenclature: string,
  ref: string,
): string {
  return buildGithubRawUrl(ref, `${schemaPath}/${nomenclature}.json`);
}
