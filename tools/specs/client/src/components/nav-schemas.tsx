import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Input } from "@/components/ui/input";
import {
  SidebarGroup,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import { preserveRefSearch } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import { SchemaReference } from "@/types";
import { Link } from "@tanstack/react-router";
import { Braces, ChevronRightIcon } from "lucide-react";
import { useMemo, useState } from "react";

export function NavSchemas() {
  const schemas = useSchemaStore((s) => s.schemas);
  const [search, setSearch] = useState<string>("");
  const [open, setOpen] = useState<boolean>(true);

  const filteredSchemas = useMemo(() => {
    const q = search.toLocaleLowerCase();
    return Object.values(schemas).filter((s) =>
      s.schemaName.toLowerCase().includes(q),
    );
  }, [search, schemas]);

  return (
    <SidebarGroup>
      <SidebarMenu>
        <Collapsible
          className="group/collapsible"
          open={open}
          onOpenChange={setOpen}
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
            {open && (
              <Input
                autoFocus
                type="search"
                placeholder="Rechercher..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="mx-2 my-1 w-[calc(100%-1rem)] group-data-[collapsible=icon]:hidden"
              />
            )}
            <SidebarMenuSub>
              {filteredSchemas.map((schema: SchemaReference) => (
                <SidebarMenuSubItem key={schema.schemaName}>
                  <SidebarMenuSubButton
                    render={
                      <Link
                        to="/$schemaName"
                        params={{ schemaName: schema.schemaName }}
                        search={preserveRefSearch}
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
