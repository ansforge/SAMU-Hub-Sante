import { create } from "zustand";

interface SchemaState {
  nomenclatureDrawerName: string | null;
  openNomenclatureDrawer: (name: string) => void;
  closeNomenclatureDrawer: () => void;
  // dotted paths of the expanded nodes in the schema field tree
  openFields: string[];
  setOpenFields: (openFields: string[]) => void;
  toggleField: (id: string) => void;
  revealField: (path: string[]) => void;
  searchOpen: boolean;
  setSearchOpen: (open: boolean) => void;
}

export const useSchemaStore = create<SchemaState>((set) => ({
  nomenclatureDrawerName: null,
  openFields: [],
  searchOpen: false,
  setSearchOpen: (searchOpen) => set({ searchOpen }),
  setOpenFields: (openFields) => set({ openFields }),
  toggleField: (id) =>
    set((s) => ({
      openFields: s.openFields.includes(id)
        ? s.openFields.filter((f) => f !== id)
        : [...s.openFields, id],
    })),
  // expands every ancestor so the field at `path` is rendered, and the field
  // itself
  revealField: (path) =>
    set((s) => ({
      openFields: [
        ...new Set([
          ...s.openFields,
          ...path.map((_, i) => path.slice(0, i + 1).join(".")),
        ]),
      ],
    })),

  openNomenclatureDrawer: (name) => set({ nomenclatureDrawerName: name }),
  closeNomenclatureDrawer: () => set({ nomenclatureDrawerName: null }),
}));
