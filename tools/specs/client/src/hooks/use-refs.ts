import { useQuery } from "@tanstack/react-query";
import { apiDomain, publicVersions } from "@/config";
import type { RepoReferences } from "@/types";
import { useAuth } from "./use-auth";

export function useRefs() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ["refs", isAuthenticated],
    queryFn: async (): Promise<RepoReferences> => {
      if (!isAuthenticated) return { tags: publicVersions };
      const res = await fetch(`${apiDomain}/refs`);
      if (!res.ok) throw new Error("not found");
      return res.json();
    },
    refetchOnMount: false,
    staleTime: 30000,
  });
}
