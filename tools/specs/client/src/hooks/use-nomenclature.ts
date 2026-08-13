import { useQuery } from "@tanstack/react-query";
import { getRouteApi } from "@tanstack/react-router";
import { buildNomenclatureUrl } from "@/lib/utils";
import type { NomenclatureSchema } from "@/types";

const rootRouteApi = getRouteApi("__root__");

export function useNomenclature(name: string) {
  const { ref } = rootRouteApi.useSearch();

  return useQuery({
    queryKey: ["nomenclature", name, ref],
    queryFn: async (): Promise<NomenclatureSchema> => {
      const res = await fetch(buildNomenclatureUrl(name, ref));
      if (!res.ok) throw new Error("not found");
      return res.json();
    },
  });
}
