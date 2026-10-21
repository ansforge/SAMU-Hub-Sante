import {
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import { getPerimeters } from "@/lib/get-perimeters";
import { Link, useParams } from "@tanstack/react-router";
import { Braces } from "lucide-react";
import { useCallback, useMemo, useState } from "react";
import { useSchemas } from "@/hooks/use-schemas";
import { NavSection } from "./nav-section";
import { PerimeterSelect } from "./perimeter-select";

export function NavSchemas() {
  const { data: schemas = [] } = useSchemas();
  const { schemaName: activeSchema } = useParams({ strict: false });
  const [search, setSearch] = useState("");
  const [selectedPerimeters, setSelectedPerimeters] = useState<string[]>([]);

  const perimeters = getPerimeters(schemas);

  const filteredSchemas = useMemo(() => {
    const q = search.toLowerCase();
    return schemas.filter((s) => {
      const matchesSearch = s.schemaName.toLowerCase().includes(q);
      if (!s.perimeters) return matchesSearch;
      const matchesPerimeters =
        selectedPerimeters.length > 0
          ? selectedPerimeters.some((p) => s.perimeters?.includes(p))
          : true;
      return matchesSearch && matchesPerimeters;
    });
  }, [search, schemas, selectedPerimeters]);

  const toggleFilter = useCallback((value: string, checked: boolean) => {
    setSelectedPerimeters((prev) =>
      checked ? [...prev, value] : prev.filter((p) => p !== value),
    );
  }, []);

  return (
    <NavSection
      title="Schemas"
      icon={<Braces />}
      defaultOpen
      search={search}
      onSearchChange={setSearch}
      toolbar={
        perimeters.length > 0 && (
          <PerimeterSelect
            toggleFilter={toggleFilter}
            selectedPerimeters={selectedPerimeters}
            perimeters={perimeters}
            className="group-data-[collapsible=icon]:hidden"
          />
        )
      }
    >
      {filteredSchemas.map((schema) => (
        <SidebarMenuSubItem key={schema.schemaName}>
          <SidebarMenuSubButton
            isActive={schema.schemaName === activeSchema}
            render={
              <Link
                to="/$schemaName"
                params={{ schemaName: schema.schemaName }}
              />
            }
          >
            <span>{schema.label}</span>
          </SidebarMenuSubButton>
        </SidebarMenuSubItem>
      ))}
    </NavSection>
  );
}
