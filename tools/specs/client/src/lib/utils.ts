import { buildGithubUrl } from "@/config";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function buildGithubSchemaUrl(
  domain: string,
  ref: string,
  schemaName: string,
): string {
  return buildGithubUrl(
    domain,
    ref,
    `src/main/resources/json-schema/${schemaName}`,
  );
}

export function buildNomenclatureUrl(
  domain: string,
  ref: string,
  nomenclature: string,
): string {
  return buildGithubUrl(
    domain,
    ref,
    `nomenclature_parser/out/latest/json_schema/${nomenclature}.json`,
  );
}

export const isMac = window.navigator.platform === "MacIntel";
