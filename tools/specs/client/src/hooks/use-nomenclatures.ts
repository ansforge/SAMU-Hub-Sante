import { useQuery } from "@tanstack/react-query";
import { getRouteApi } from "@tanstack/react-router";
import { nomenclatureListUrl } from "@/config";
import { fetchGithubList, githubListQueryOptions } from "@/lib/github-list";
import type { NomenclatureReference } from "@/types";

const rootRouteApi = getRouteApi("__root__");

export function useNomenclatures() {
  const { ref } = rootRouteApi.useSearch();
  return useQuery({
    queryKey: ["nomenclatures", ref],
    queryFn: async () =>
      (await fetchGithubList<NomenclatureReference>(
        nomenclatureListUrl(ref),
      )) ?? [],
    ...githubListQueryOptions,
  });
}
