import { keepPreviousData } from "@tanstack/react-query";

export const githubListQueryOptions = {
  placeholderData: keepPreviousData,
  retry: false,
  staleTime: 30_000,
} as const;

export async function fetchGithubList<T>(
  url: string,
): Promise<T[] | null> {
  const res = await fetch(url);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Échec du chargement (HTTP ${res.status})`);
  const list: T[] = await res.json();
  return list;
}
