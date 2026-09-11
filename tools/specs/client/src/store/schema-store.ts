import { create } from "zustand";
import { type SchemaReference } from "@/types";

interface SchemaState {
  schemas: Record<string, SchemaReference>;
  loadedRef: string | null;
  selectedName: string | null;
  setSchemasFromArray: (schemas: SchemaReference[], ref: string) => void;
  selectSchema: (name: string) => void;
  getSchema: (name: string) => SchemaReference | undefined;
  nomenclatureDrawerName: string | null;
  openNomenclatureDrawer: (name: string) => void;
  closeNomenclatureDrawer: () => void;
  // bumped on every "expand/collapse all" click (or a search-palette field
  // jump), forcing every Accordion in schema-fields.tsx to remount with the
  // new defaultValue instead of staying uncontrolled but stuck on old state
  expandSignal: { key: number; expand: boolean };
  toggleExpandAll: () => void;
  expandAll: () => void;
}

export const useSchemaStore = create<SchemaState>((set, get) => ({
  schemas: {},
  loadedRef: null,
  selectedName: null,
  nomenclatureDrawerName: null,
  expandSignal: { key: 0, expand: false },
  toggleExpandAll: () =>
    set((s) => ({
      expandSignal: {
        key: s.expandSignal.key + 1,
        expand: !s.expandSignal.expand,
      },
    })),
  expandAll: () =>
    set((s) => ({
      expandSignal: { key: s.expandSignal.key + 1, expand: true },
    })),

  setSchemasFromArray: (schemas, ref) =>
    set({
      schemas: Object.fromEntries(
        schemas.map((schema) => [schema.schemaName, schema]),
      ),
      loadedRef: ref,
      selectedName: get().selectedName ?? schemas[0]?.schemaName ?? null,
    }),

  selectSchema: (name) => set({ selectedName: name }),
  getSchema: (name) => get().schemas[name],

  openNomenclatureDrawer: (name) => set({ nomenclatureDrawerName: name }),
  closeNomenclatureDrawer: () => set({ nomenclatureDrawerName: null }),
}));
