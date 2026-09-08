import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { repo, schemaPath, nomenclaturePath } from "@/config";
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
  const str = buildGithubRawUrl(
    ref,
    `${nomenclaturePath}/${nomenclature}.json`,
  );
  console.log(str);
  return buildGithubRawUrl(ref, `${nomenclaturePath}/${nomenclature}.json`);
}
