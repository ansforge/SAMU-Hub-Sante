import { useAuth } from "@/hooks/use-auth";
import {
  BadgeCheckIcon,
  BellIcon,
  CreditCardIcon,
  LogIn,
  LogOutIcon,
} from "lucide-react";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { Spinner } from "@/components/ui/spinner";

function User() {
  const { user, login, logout, isLoading, isAuthenticated } = useAuth();

  if (isLoading)
    return (
      <div className="h-8 w-8 flex items-center justify-center border rounded-full opacity-60">
        <Spinner />
      </div>
    );
  if (!isAuthenticated)
    return (
      <Button
        onClick={login}
        variant="ghost"
        size="icon"
        className="rounded-full"
      >
        <LogIn />
      </Button>
    );

  const avatarFallback = user.username?.[0] ?? "?";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button variant="ghost" size="icon" className="rounded-full">
            <Avatar>
              <AvatarImage src={user.avatarUrl} />
              <AvatarFallback>{avatarFallback}</AvatarFallback>
            </Avatar>
          </Button>
        }
      />
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={logout} className={"px-1"}>
          <LogOutIcon />
          Déconnnexion
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export default User;
