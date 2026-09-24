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
import { getPerimeters } from "@/lib/get-perimeters";
import { useSchemaStore } from "@/store/schema-store";
import { SchemaReference } from "@/types";
import { Link } from "@tanstack/react-router";
import { Braces, ChevronRightIcon } from "lucide-react";
import { useCallback, useMemo, useState } from "react";
import { PerimeterSelect } from "./perimeter-select";

export function NavSchemas() {
  const schemas = useSchemaStore((s) => s.schemas);
  const [search, setSearch] = useState<string>("");
  const [open, setOpen] = useState<boolean>(true);
  const [selectedPerimeters, seSelectedPerimeters] = useState<string[]>([]);

  const perimeters = getPerimeters(Object.values(schemas));

  const filteredSchemas = useMemo(() => {
    const q = search.toLocaleLowerCase();
    return Object.values(schemas).filter((s) => {
      const matchesSearch = s.schemaName
        .toLowerCase()
        .includes(q.toLowerCase());
      if (!s.perimeters) return matchesSearch;
      const matchesPerimeters =
        selectedPerimeters.length > 0
          ? selectedPerimeters.some((p) => s.perimeters?.includes(p))
          : true;
      return matchesSearch && matchesPerimeters;
    });
  }, [search, schemas, selectedPerimeters]);

  const toggleFilter = useCallback(
    (value: string, checked: boolean) => {
      seSelectedPerimeters((prev) => {
        if (checked) return [...prev, value];
        else return prev.filter((p) => p !== value);
      });
    },
    [seSelectedPerimeters],
  );

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
              <div className="flex items-center gap-1">
                <Input
                  autoFocus
                  type="search"
                  placeholder="Rechercher..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="mx-2 my-1 w-[calc(100%-1rem)] group-data-[collapsible=icon]:hidden grow"
                />
                {perimeters.length > 0 && (
                  <PerimeterSelect
                    toggleFilter={toggleFilter}
                    selectedPerimeters={selectedPerimeters}
                    perimeters={perimeters}
                    className="group-data-[collapsible=icon]:hidden"
                  />
                )}
              </div>
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
