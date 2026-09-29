import * as React from "react";

import { NavSchemas } from "@/components/nav-schemas";
import {
  Sidebar,
  SidebarContent,
  SidebarRail,
} from "@/components/ui/sidebar";

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarContent>
        <NavSchemas />
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  );
}
