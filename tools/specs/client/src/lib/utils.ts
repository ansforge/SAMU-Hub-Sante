import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

function buildGithubRawUrl(ref: string, path: string): string {
  return `https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/${ref}/${path}`;
}

export function buildGithubSchemaUrl(schemaName: string, ref: string): string {
  return buildGithubRawUrl(ref, `src/main/resources/json-schema/${schemaName}`);
}

export function buildNomenclatureUrl(nomenclature: string, ref: string): string {
  return buildGithubRawUrl(
    ref,
    `nomenclature_parser/out/latest/json_schema/${nomenclature}.json`,
  );
}
