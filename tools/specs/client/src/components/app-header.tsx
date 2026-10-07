import { Separator } from "@base-ui/react";
import { Link } from "@tanstack/react-router";

import { AssetImage } from "@/components/asset-image";
import GlobalSearch from "@/components/global-search";
import RefSelector from "@/components/ref-selector";
import User from "@/components/user";
import { SidebarTrigger } from "./ui/sidebar";

export function AppHeader() {
  return (
    <header className="sticky top-0 z-20 flex h-(--header-height) w-full shrink-0 items-center gap-4 border-b bg-background px-4">
      <div className="block lg:hidden">
        <SidebarTrigger />
      </div>
      <a
        href="https://sante.gouv.fr/"
        target="_blank"
        rel="noopener noreferrer"
        className="hidden lg:block"
      >
        <AssetImage
          name="logo-ministere.svg"
          alt="Ministère de la Santé et de la Prévention (nouvelle fenêtre)"
          className="h-14"
        />
      </a>
      <Link to="/">
        <AssetImage name="logo-ANS.svg" alt="Accueil ANS" className="h-11" />
      </Link>
      <Separator orientation="vertical" className="mx-2 h-6 w-px bg-border" />
      <h1 className="lg:text-xl text-md font-bold truncate text-ellipsis">
        Hub Santé - Spécifications
      </h1>
      <RefSelector />
      <div className="grow" />
      <GlobalSearch />
      <User />
    </header>
  );
}
