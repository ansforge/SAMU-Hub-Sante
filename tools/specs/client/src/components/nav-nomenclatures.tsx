import {
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import { Type } from "lucide-react";
import { useMemo, useState } from "react";
import { useNomenclatures } from "@/hooks/use-nomenclatures";
import { useSchemaStore } from "@/store/schema-store";
import { NavSection } from "./nav-section";

export function NavNomenclatures() {
  const { data: nomenclatures = [], error } = useNomenclatures();
  const [search, setSearch] = useState("");
  const openNomenclatureDrawer = useSchemaStore(
    (s) => s.openNomenclatureDrawer,
  );

  const filteredNomenclatures = useMemo(() => {
    const q = search.toLowerCase();
    return nomenclatures.filter((n) => n.label.toLowerCase().includes(q));
  }, [nomenclatures, search]);

  if (!nomenclatures.length && !error) return null;

  return (
    <NavSection
      title="Nomenclatures"
      icon={<Type />}
      search={search}
      onSearchChange={setSearch}
      error={error}
    >
      {filteredNomenclatures.map((n) => (
        <SidebarMenuSubItem key={n.schemaName}>
          <SidebarMenuSubButton
            className="cursor-pointer"
            onClick={() => openNomenclatureDrawer(n.label)}
          >
            <span>{n.label}</span>
          </SidebarMenuSubButton>
        </SidebarMenuSubItem>
      ))}
    </NavSection>
  );
}
