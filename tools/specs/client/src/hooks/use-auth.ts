import { apiDomain } from "@/config";
import { type AuthResponse, ApiAuthResponse } from "@/types";
import { queryOptions, useQuery, useQueryClient } from "@tanstack/react-query";

export async function fetchCurrentUser(): Promise<AuthResponse> {
  const res = await fetch(`${apiDomain}/auth/me`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    throw new Error("Not authenticated.");
  }

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
  staleTime: 1000 * 60 * 5,
});

export function useAuth() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError, refetch } = useQuery(authQueryOptions);

  const login = () => {
    window.location.href = `${apiDomain}/auth/github/login`;
  };

  const logout = async () => {
    try {
      await fetch(`${apiDomain}/auth/logout`, {
        method: "POST",
        credentials: "include",
      });
    } finally {
      queryClient.setQueryData<AuthResponse>(["auth", "me"], {
        isAuthenticated: false,
        user: null,
      });
    }
  };

  const auth: AuthResponse = data ?? { isAuthenticated: false, user: null };

  return { isLoading, isError, login, logout, refetchAuth: refetch, ...auth };
}
