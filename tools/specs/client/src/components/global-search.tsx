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
import { fieldTitle, flattenFields } from "@/components/schema-detail/schema-utils";
import { useMatch, useNavigate } from "@tanstack/react-router";
import { useCallback, useMemo } from "react";
import { useSchemas } from "@/hooks/use-schemas";
import { SearchIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { isMac } from "@/lib/utils";

// normalize is a method to remove all accent
// so it does something like : é -> e
// so that if you type "perimetre" it matches "périmètre"
const normalize = (s: string) =>
  s
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase();

// we override the search filter to be able to filter by title and label
const filter = (value: string, search: string, keywords: string[] = []) => {
  const query = normalize(search).trim();
  const haystack = normalize([value, ...keywords].join(" "));
  if (!query.split(/\s+/).every((word) => haystack.includes(word))) return 0;
  const v = normalize(value);
  if (v === query || v.endsWith(`.${query}`)) return 1;
  return v.includes(query) ? 0.8 : 0.5;
};

const GlobalSearch = () => {
  const open = useSchemaStore((s) => s.searchOpen);
  const setOpen = useSchemaStore((s) => s.setSearchOpen);
  const { data: schemas = [] } = useSchemas();
  const navigate = useNavigate();
  const currentSchemaMatch = useMatch({
    from: "/$schemaName",
    shouldThrow: false,
  });
  const currentSchema = currentSchemaMatch?.loaderData;
  const currentSchemaLabel = schemas.find(
    (s) => s.schemaName === currentSchemaMatch?.params.schemaName,
  )?.label;
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

  const toggleGlobalSearch = useCallback(
    () => setOpen(!useSchemaStore.getState().searchOpen),
    [],
  );

  const handleOnSelect = useCallback((schemaName: string) => {
    setOpen(false);
    navigate({
      to: "/$schemaName",
      params: { schemaName },
    });
  }, []);

  const handleFieldSelect = useCallback((fieldPath: string) => {
    useSchemaStore.getState().revealField(fieldPath.split("."));
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
    <>
      <Button
        variant="outline"
        onClick={() => setOpen(true)}
        className="w-auto xl:w-64 justify-start gap-2 text-muted-foreground"
      >
        <SearchIcon />
        <div className="hidden xl:flex items-center justify-between w-full">
          <span className="grow text-left">Rechercher...</span>
          <kbd className="rounded border bg-background px-1.5 text-xs">
            {isMac ? "⌘K" : "Ctrl+K"}
          </kbd>
        </div>
      </Button>
      <CommandDialog open={open} onOpenChange={setOpen}>
        <Command filter={filter}>
          <CommandInput placeholder="Rechercher..." />
          <CommandList>
            <CommandEmpty>Aucun résultat trouvé.</CommandEmpty>
            {flatFields.length > 0 && (
              <CommandGroup heading={`Champs du ${currentSchemaLabel}`}>
                {flatFields.map(({ path, prop }) => {
                  const fieldPath = path.join(".");
                  const title = fieldTitle(prop, currentSchemaDefinitions);
                  return (
                    <CommandItem
                      key={fieldPath}
                      value={fieldPath}
                      keywords={title ? [title] : []}
                      onSelect={() => handleFieldSelect(fieldPath)}
                    >
                      <div className="flex min-w-0 flex-col">
                        <span>{fieldPath}</span>
                        {title && (
                          <span className="truncate text-xs text-muted-foreground">
                            {title}
                          </span>
                        )}
                      </div>
                    </CommandItem>
                  );
                })}
              </CommandGroup>
            )}
            <CommandGroup heading="Schemas">
              {schemas.map((s) => (
                <CommandItem
                  key={s.schemaName}
                  value={s.label}
                  keywords={[s.schemaName]}
                  onSelect={() => handleOnSelect(s.schemaName)}
                >
                  {s.label}
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </CommandDialog>
    </>
  );
};
export default GlobalSearch;
