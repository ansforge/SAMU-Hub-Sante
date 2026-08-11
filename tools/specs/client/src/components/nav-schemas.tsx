import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  SidebarGroup,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import { defaultRef } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import { SchemaReference } from "@/types";
import { Link } from "@tanstack/react-router";
import { Braces, ChevronRightIcon } from "lucide-react";

export function NavSchemas() {
  const schemas = useSchemaStore((s) => s.schemas);
  return (
    <SidebarGroup>
      <SidebarMenu>
        <Collapsible
          className="group/collapsible"
          defaultOpen={true}
          render={<SidebarMenuItem />}
        >
          <CollapsibleTrigger
            render={<SidebarMenuButton tooltip={"Schemas"} />}
          >
            <Braces />
            <span>Schemas</span>
            <ChevronRightIcon className="ml-auto transition-transform duration-200 group-data-open/collapsible:rotate-90" />
          </CollapsibleTrigger>
          <CollapsibleContent>
            <SidebarMenuSub>
              {Object.values(schemas).map((schema: SchemaReference) => (
                <SidebarMenuSubItem key={schema.schemaName}>
                  <SidebarMenuSubButton
                    render={
                      <Link
                        to="/$schemaName"
                        params={{ schemaName: schema.schemaName }}
                        search={(prev) => ({ ref: prev.ref ?? defaultRef })}
                      />
                    }
                  >
                    <span>{schema.label}</span>
                  </SidebarMenuSubButton>
                </SidebarMenuSubItem>
              ))}
            </SidebarMenuSub>
          </CollapsibleContent>
        </Collapsible>
      </SidebarMenu>
    </SidebarGroup>
  );
}
