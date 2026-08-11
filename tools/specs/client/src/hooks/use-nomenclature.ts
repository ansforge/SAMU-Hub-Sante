import { useQuery } from "@tanstack/react-query";
import { defaultBranch } from "@/config";
import { buildNomenclatureUrl } from "@/lib/utils";
import type { NomenclatureSchema } from "@/types";

export function useNomenclature(name: string) {
  return useQuery({
    queryKey: ["nomenclature", name],
    queryFn: async (): Promise<NomenclatureSchema> => {
      const res = await fetch(buildNomenclatureUrl(name, defaultBranch));
      if (!res.ok) throw new Error("not found");
      return res.json();
    },
  });
}
