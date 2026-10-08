import { create } from "zustand";

interface SchemaState {
  nomenclatureDrawerName: string | null;
  openNomenclatureDrawer: (name: string) => void;
  closeNomenclatureDrawer: () => void;
  // bumped on every "expand/collapse all" click (or a search-palette field
  // jump), forcing every Accordion in schema-fields.tsx to remount with the
  // new defaultValue instead of staying uncontrolled but stuck on old state
  expandSignal: { key: number; expand: boolean };
  toggleExpandAll: () => void;
  expandAll: () => void;
  searchOpen: boolean;
  setSearchOpen: (open: boolean) => void;
}

export const useSchemaStore = create<SchemaState>((set) => ({
  nomenclatureDrawerName: null,
  expandSignal: { key: 0, expand: false },
  searchOpen: false,
  setSearchOpen: (searchOpen) => set({ searchOpen }),
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

  openNomenclatureDrawer: (name) => set({ nomenclatureDrawerName: name }),
  closeNomenclatureDrawer: () => set({ nomenclatureDrawerName: null }),
}));
