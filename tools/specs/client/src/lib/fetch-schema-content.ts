import { rawGithubDomain } from "@/config";
import { buildGithubSchemaUrl } from "@/lib/utils";

export async function fetchSchemaContent(
  schemaName: string,
  ref: string,
): Promise<string> {
  const res = await fetch(
    buildGithubSchemaUrl(rawGithubDomain, ref, schemaName),
  );

  if (!res.ok) {
    throw new Error(
      res.status === 404
        ? "not-found"
        : `Échec du chargement (HTTP ${res.status})`,
    );
  }
  return res.text();
}
