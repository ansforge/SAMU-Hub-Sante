import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { getRouteApi } from "@tanstack/react-router";
import { messageListUrl, vhostPerimeters } from "@/config";
import type { SchemaReference } from "@/types";

const rootRouteApi = getRouteApi("__root__");

export function useSchemas() {
  const { ref } = rootRouteApi.useSearch();
  return useQuery({
    queryKey: ["schemas", ref],
    queryFn: async (): Promise<SchemaReference[]> => {
      const res = await fetch(messageListUrl(ref));
      if (res.status === 404) {
        throw new Error(`Branche ou tag "${ref}" introuvable.`);
      }
      if (!res.ok) throw new Error(`Échec du chargement (HTTP ${res.status})`);
      const schemas: SchemaReference[] = await res.json();
      const perimeters = vhostPerimeters(ref);
      if (!perimeters) return schemas;
      return schemas.map((schema) => ({
        ...schema,
        perimeters: [...perimeters]
          .filter(([, labels]) => labels.has(schema.label))
          .map(([perimeter]) => perimeter),
      }));
    },
    placeholderData: keepPreviousData,
    retry: false,
    staleTime: 30_000,
  });
}
