import { useQuery } from "@tanstack/react-query";
import { getRouteApi } from "@tanstack/react-router";
import { messageListUrl, messagePerimeters } from "@/config";
import { fetchGithubList, githubListQueryOptions } from "@/lib/github-list";
import type { SchemaReference } from "@/types";

const rootRouteApi = getRouteApi("__root__");

export function useSchemas() {
  const { ref } = rootRouteApi.useSearch();
  return useQuery({
    queryKey: ["schemas", ref],
    queryFn: async (): Promise<SchemaReference[]> => {
      const schemas = await fetchGithubList<SchemaReference>(
        messageListUrl(ref),
      );
      if (!schemas) throw new Error(`Branche ou tag "${ref}" introuvable.`);
      return schemas.map((schema) => ({
        ...schema,
        perimeters: messagePerimeters(schema.label),
      }));
    },
    ...githubListQueryOptions,
  });
}
