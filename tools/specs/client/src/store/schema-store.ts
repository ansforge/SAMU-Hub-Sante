import { create } from "zustand";
import { type SchemaReference } from "@/types";

interface SchemaState {
  schemas: Record<string, SchemaReference>;
  selectedName: string | null;
  setSchemasFromArray: (schemas: SchemaReference[]) => void;
  selectSchema: (name: string) => void;
  getSchema: (name: string) => SchemaReference | undefined;
}

export const useSchemaStore = create<SchemaState>((set, get) => ({
  schemas: {},
  selectedName: null,

  setSchemasFromArray: (schemas) =>
    set({
      schemas: Object.fromEntries(
        schemas.map((schema) => [schema.schemaName, schema]),
      ),
      selectedName: get().selectedName ?? schemas[0]?.schemaName ?? null,
    }),

  selectSchema: (name) => set({ selectedName: name }),
  getSchema: (name) => get().schemas[name],
}));
