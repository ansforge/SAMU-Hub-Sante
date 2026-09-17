import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { useKeyboardShortcut } from "@/hooks/use-keyboard-shortcut";
import { useSchemaStore } from "@/store/schema-store";
import { preserveRefSearch } from "@/config";
import { flattenFields } from "@/components/schema-detail/schema-utils";
import { useMatch, useNavigate } from "@tanstack/react-router";
import { useCallback, useMemo, useState } from "react";

const GlobalSearch = () => {
  const [open, setOpen] = useState(false);
  const schemas = useSchemaStore((s) => s.schemas);
  const navigate = useNavigate();
  const currentSchemaMatch = useMatch({
    from: "/$schemaName",
    shouldThrow: false,
  });
  const currentSchema = currentSchemaMatch?.loaderData;
  const currentSchemaFields = currentSchema?.properties;
  const currentSchemaDefinitions =
    currentSchema?.definitions ?? currentSchema?.$defs ?? {};
  const flatFields = useMemo(
    () =>
      currentSchemaFields
        ? flattenFields(currentSchemaFields, currentSchemaDefinitions)
        : [],
    [currentSchemaFields, currentSchemaDefinitions],
  );

  const toggleGlobalSearch = useCallback(() => setOpen((prev) => !prev), []);

  const handleOnSelect = useCallback((schemaName: string) => {
    setOpen(false);
    navigate({
      to: "/$schemaName",
      params: { schemaName },
      search: preserveRefSearch,
    });
  }, []);

  const handleFieldSelect = useCallback((fieldPath: string) => {
    useSchemaStore.getState().expandAll();
    setOpen(false);
    // wait for the close animation + accordion expansion to settle before
    // scrolling, instead of scrolling while still mid-close
    setTimeout(() => {
      window.location.hash = fieldPath;
      document.getElementById(fieldPath)?.scrollIntoView({ block: "start" });
    }, 150);
  }, []);

  useKeyboardShortcut({
    key: "k",
    ctrlOrCmd: true,
    allowInEditable: true,
    callback: toggleGlobalSearch,
  });

  return (
    <CommandDialog open={open} onOpenChange={setOpen}>
      <Command>
        <CommandInput placeholder="Rechercher..." />
        <CommandList>
          <CommandEmpty>Aucun résultat trouvé.</CommandEmpty>
          {flatFields.length > 0 && (
            <CommandGroup heading={`Champs du ${currentSchema?.title}`}>
              {flatFields.map(({ path }) => {
                const fieldPath = path.join(".");
                return (
                  <CommandItem
                    key={fieldPath}
                    onSelect={() => handleFieldSelect(fieldPath)}
                  >
                    {fieldPath}
                  </CommandItem>
                );
              })}
            </CommandGroup>
          )}
          <CommandGroup heading="Schemas">
            {Object.values(schemas).map((s) => (
              <CommandItem
                key={s.schemaName}
                onSelect={() => handleOnSelect(s.schemaName)}
              >
                {s.label}
              </CommandItem>
            ))}
          </CommandGroup>
        </CommandList>
      </Command>
    </CommandDialog>
  );
};
export default GlobalSearch;
