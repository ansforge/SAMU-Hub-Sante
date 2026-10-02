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

export function buildGithubSchemaExampleUrl(
  domain: string,
  ref: string,
  schemaExampleName: string,
): string {
  return buildGithubUrl(
    domain,
    ref,
    `src/main/resources/sample/examples/${schemaExampleName}`,
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

// csv_parser writes the generated example next to its other outputs
export function buildCsvParserExampleUrl(
  domain: string,
  ref: string,
  schemaName: string,
): string {
  const name = schemaName.replace(/\.schema\.json$/, "");
  return buildGithubUrl(
    domain,
    ref,
    `csv_parser/out/${name}/${name}.example.json`,
  );
}

// trailing slash leaves the root "example.json#" alone: resolving it would
// copy the whole example onto the schema root
const EXAMPLE_POINTER_PREFIX = "example.json#/";

// "example.json#/a/0/b" → value at JSON pointer /a/0/b of the example doc
function resolveExamplePointer(example: unknown, pointer: string): unknown {
  return pointer
    .slice(EXAMPLE_POINTER_PREFIX.length)
    .split("/")
    .map((s) => s.replace(/~1/g, "/").replace(/~0/g, "~"))
    .reduce<unknown>(
      (node, key) =>
        node && typeof node === "object"
          ? (node as Record<string, unknown>)[key]
          : undefined,
      example,
    );
}

export function inlineExamples<T>(node: T, example: unknown): T {
  if (Array.isArray(node))
    return node.map((n) => inlineExamples(n, example)) as T;
  if (!node || typeof node !== "object") return node;
  return Object.fromEntries(
    Object.entries(node).map(([k, v]) => [
      k,
      k === "example" &&
      typeof v === "string" &&
      v.startsWith(EXAMPLE_POINTER_PREFIX)
        ? resolveExamplePointer(example, v)
        : inlineExamples(v, example),
    ]),
  ) as T;
}
