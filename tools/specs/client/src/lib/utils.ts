import { buildGithubUrl } from "@/config";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { schemaPath, nomenclaturePath } from "@/config";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function buildGithubSchemaUrl(
  domain: string,
  ref: string,
  schemaName: string,
): string {
  return buildGithubUrl(domain, ref, `${schemaPath}/${schemaName}`);
}

export function buildNomenclatureUrl(
  domain: string,
  ref: string,
  nomenclature: string,
): string {
  return buildGithubUrl(
    domain,
    ref,
    `${nomenclaturePath}/${nomenclature}.json`,
  );
}
