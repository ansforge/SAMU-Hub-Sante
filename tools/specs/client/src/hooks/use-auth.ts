import { apiDomain, resolveDefaultRef } from "@/config";
import { type AuthResponse, ApiAuthResponse } from "@/types";
import { queryOptions, useQuery } from "@tanstack/react-query";

export async function fetchCurrentUser(): Promise<AuthResponse> {
  const res = await fetch(`${apiDomain}/auth/me`, {
    method: "GET",
    credentials: "include",
  });

  if (res.status === 401) return { isAuthenticated: false, user: null };
  if (!res.ok) throw new Error(`Auth check failed (HTTP ${res.status})`);

  const data: ApiAuthResponse = await res.json();

  if (!data.authenticated) return { isAuthenticated: false, user: null };

  return {
    isAuthenticated: true,
    user: {
      username: data.user.username,
      avatarUrl: data.user.avatar_url,
    },
  };
}

export const authQueryOptions = queryOptions({
  queryKey: ["auth", "me"],
  queryFn: fetchCurrentUser,
  retry: false,
  // an errored query refetches on every observer mount, and App unmounts the
  // router while loading: without this, a down API loops forever
  retryOnMount: false,
  staleTime: 1000 * 60 * 5,
});

export function useAuth() {
  const { data, isLoading } = useQuery(authQueryOptions);

  const login = () => {
    window.location.href = `${apiDomain}/auth/github/login`;
  };

  // full reload, like login: auth, route guards and caches all start fresh
  const logout = async () => {
    try {
      await fetch(`${apiDomain}/auth/logout`, {
        method: "POST",
        credentials: "include",
      });
    } finally {
      window.location.reload();
    }
  };

  const auth: AuthResponse = data ?? { isAuthenticated: false, user: null };

  return { isLoading, login, logout, ...auth };
}

export function useDefaultRef() {
  return resolveDefaultRef(useAuth().isAuthenticated);
}
