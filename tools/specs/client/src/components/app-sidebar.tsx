import * as React from "react";

import { NavSchemas } from "@/components/nav-schemas";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar";

import { Link } from "@tanstack/react-router";

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <Link to="/">
          <img
            src={`${import.meta.env.BASE_URL}logo-ANS-footer.svg`}
            alt="ANS"
            className="h-10"
          />
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <NavSchemas />
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  );
}
