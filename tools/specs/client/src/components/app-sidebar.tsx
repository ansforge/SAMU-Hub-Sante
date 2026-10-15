import * as React from "react";

import { NavSchemas } from "@/components/nav-schemas";
import { Sidebar, SidebarContent, SidebarRail } from "@/components/ui/sidebar";
import { NavNomenclatures } from "./nav-nomenclatures";

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarContent>
        <NavSchemas />
        <NavNomenclatures />
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  );
}
