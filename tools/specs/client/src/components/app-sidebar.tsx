import * as React from "react";

import { AssetImage } from "@/components/asset-image";
import { NavSchemas } from "@/components/nav-schemas";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar";

import { preserveRefSearch } from "@/config";
import { Link } from "@tanstack/react-router";

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <Link to="/" search={preserveRefSearch}>
          <AssetImage name="logo-ANS-footer.svg" alt="ANS" size={40} />
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <NavSchemas />
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  );
}
