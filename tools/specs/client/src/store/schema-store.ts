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
}

export const useSchemaStore = create<SchemaState>((set, get) => ({
  schemas: {},
  loadedRef: null,
  selectedName: null,
  nomenclatureDrawerName: null,

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
