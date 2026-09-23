import { Separator } from "@base-ui/react";
import { Link } from "@tanstack/react-router";

import { AssetImage } from "@/components/asset-image";
import GlobalSearch from "@/components/global-search";
import RefSelector from "@/components/ref-selector";
import User from "@/components/user";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { preserveRefSearch } from "@/config";

export function AppHeader() {
  return (
    <header className="sticky top-0 z-20 flex h-(--header-height) w-full shrink-0 items-center gap-4 border-b bg-background px-4">
      <a href="https://solidarites-sante.gouv.fr/">
        <AssetImage
          name="logo-ministere.svg"
          alt="Ministère de la Santé et de la Prévention"
          className="h-14"
        />
      </a>
      <Link to="/" search={preserveRefSearch}>
        <AssetImage name="logo-ANS.svg" alt="Accueil ANS" className="h-11" />
      </Link>
      <Separator orientation="vertical" className="mx-2 h-6 w-px bg-border" />
      <SidebarTrigger />
      <RefSelector />
      <div className="grow" />
      <GlobalSearch />
      <User />
    </header>
  );
}
