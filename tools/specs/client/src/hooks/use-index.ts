import { rawGithubDomain } from "@/config";
import { githubListQueryOptions } from "@/lib/github-list";
import { buildSchemaIndexUrl } from "@/lib/utils";
import { SchemaIndex } from "@/types";
import { useQuery } from "@tanstack/react-query";
import { getRouteApi } from "@tanstack/react-router";

const rootRouteApi = getRouteApi("__root__");

// stable reference so react-query doesn't re-flatten on every render
const flattenIndex = (data: SchemaIndex | null) =>
  data ? Object.values(data).flat() : [];

export function useSchemaIndex() {
  const { ref } = rootRouteApi.useSearch();

  return useQuery({
    queryKey: ["schema-index", ref],
    queryFn: async (): Promise<SchemaIndex | null> => {
      const res = await fetch(buildSchemaIndexUrl(rawGithubDomain, ref));
      // older refs don't publish the index
      if (res.status === 404) return null;
      if (!res.ok) throw new Error(`Échec du chargement (HTTP ${res.status})`);
      return res.json();
    },
    select: flattenIndex,
    ...githubListQueryOptions,
  });
}
