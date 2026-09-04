import { useQuery } from "@tanstack/react-query";
import { apiDomain } from "@/config";
import type { RepoReferences } from "@/types";

export function useRefs() {
  return useQuery({
    queryKey: ["refs"],
    queryFn: async (): Promise<RepoReferences> => {
      const res = await fetch(`${apiDomain}/repo/refs`);
      if (!res.ok) throw new Error("not found");
      return res.json();
    },
    refetchOnMount: false,
    staleTime: 30000,
  });
}
