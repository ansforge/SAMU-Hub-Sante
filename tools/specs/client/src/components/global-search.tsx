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
import {
  fieldTitle,
  flattenFields,
} from "@/components/schema-detail/schema-utils";
import { useMatch, useNavigate } from "@tanstack/react-router";
import { useCallback, useMemo, useState } from "react";
import { useSchemas } from "@/hooks/use-schemas";
import { Braces, SearchIcon, Type } from "lucide-react";
import { Button } from "@/components/ui/button";
import { isMac } from "@/lib/utils";
import { useNomenclatures } from "@/hooks/use-nomenclatures";
import { useSchemaIndex } from "@/hooks/use-index";
import { Badge } from "./ui/badge";

// normalize is a method to remove all accent
// so it does something like : é -> e
// so that if you type "perimetre" it matches "périmètre"
const normalize = (s: string) =>
  s
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase();

// cmdk's built-in filter renders every item and renormalizes each one on
// every keystroke (~1000 with the index), so we filter ourselves on
// pre-normalized strings and only render the best matches
const MAX_RESULTS = 50;

type Entry<T> = { item: T; value: string; v: string; haystack: string };

const toEntries = <T,>(
  items: T[],
  value: (item: T) => string,
  keywords: (item: T) => (string | null | undefined)[],
): Entry<T>[] =>
  items.map((item) => {
    const val = value(item);
    return {
      item,
      value: val,
      v: normalize(val),
      haystack: normalize([val, ...keywords(item)].filter(Boolean).join(" ")),
    };
  });

// query is already normalized and trimmed
const score = ({ v, haystack }: Entry<unknown>, query: string) => {
  if (!query.split(/\s+/).every((word) => haystack.includes(word))) return 0;
  if (v === query || v.endsWith(`.${query}`)) return 1;
  return v.includes(query) ? 0.8 : 0.5;
};

const match = <T,>(entries: Entry<T>[], query: string): Entry<T>[] =>
  query
    ? entries
        .map((entry) => ({ entry, score: score(entry, query) }))
        .filter((r) => r.score > 0)
        .sort((a, b) => b.score - a.score)
        .slice(0, MAX_RESULTS)
        .map((r) => r.entry)
    : entries;

// cmdk doesn't highlight matches, so we mark every occurrence of each query word
const Highlight = ({ text, query }: { text: string; query: string }) => {
  const words = query.split(/\s+/).filter(Boolean);
  const hay = normalize(text);
  // positions only line up when normalize keeps length (true for precomposed accents)
  if (!words.length || hay.length !== text.length) return text;
  const marked = new Array<boolean>(text.length).fill(false);
  for (const w of words)
    for (let i = hay.indexOf(w); i !== -1; i = hay.indexOf(w, i + 1))
      marked.fill(true, i, i + w.length);
  // bridge whitespace between two hits so "nature de fait" is one mark
  for (let i = 1; i < text.length; i++) {
    if (marked[i] || !marked[i - 1] || !/\s/.test(text[i])) continue;
    let j = i;
    while (j < text.length && /\s/.test(text[j])) j++;
    if (marked[j]) marked.fill(true, i, j);
  }
  const parts: { str: string; hit: boolean }[] = [];
  for (let i = 0; i < text.length; i++) {
    const last = parts[parts.length - 1];
    if (last?.hit === marked[i]) last.str += text[i];
    else parts.push({ str: text[i], hit: marked[i] });
  }
  return (
    <span>
      {parts.map((p, i) =>
        p.hit ? (
          <mark key={i} className="rounded-sm bg-primary/20 text-inherit">
            {p.str}
          </mark>
        ) : (
          p.str
        ),
      )}
    </span>
  );
};

const GlobalSearch = () => {
  const open = useSchemaStore((s) => s.searchOpen);
  const setOpen = useSchemaStore((s) => s.setSearchOpen);
  const { data: schemas = [] } = useSchemas();
  const { data: nomenclatures = [] } = useNomenclatures();
  const { data: schemaIndex = [] } = useSchemaIndex();
  const [search, setSearch] = useState("");

  const openNomenclatureDrawer = useSchemaStore(
    (s) => s.openNomenclatureDrawer,
  );

  const navigate = useNavigate();
  const currentSchemaMatch = useMatch({
    from: "/$schemaName",
    shouldThrow: false,
  });
  const currentSchema = currentSchemaMatch?.loaderData?.schema;
  const currentSchemaLabel = schemas.find(
    (s) => s.schemaName === currentSchemaMatch?.params.schemaName,
  )?.label;
  // normalized once per data change, not on every keystroke
  const entries = useMemo(() => {
    const definitions =
      currentSchema?.definitions ?? currentSchema?.$defs ?? {};
    const fields = currentSchema?.properties
      ? flattenFields(currentSchema.properties, definitions).map(
          ({ path, prop }) => ({
            fieldPath: path.join("."),
            title: fieldTitle(prop, definitions),
          }),
        )
      : [];
    return {
      fields: toEntries(
        fields,
        (f) => f.fieldPath,
        (f) => [f.title],
      ),
      schemas: toEntries(
        schemas,
        (s) => s.label,
        (s) => [s.schemaName],
      ),
      // the current schema's fields are already listed above
      index: toEntries(
        schemaIndex.filter((s) => s.schema !== currentSchemaLabel),
        (s) => `${s.schema}-${s.path}`,
        (s) => [s.schemaLabel, s.schema, s.path, s.schemaTitle],
      ),
      nomenclatures: toEntries(
        nomenclatures,
        (n) => n.label,
        (n) => [n.schemaName],
      ),
    };
  }, [currentSchema, schemas, schemaIndex, nomenclatures, currentSchemaLabel]);

  const query = normalize(search).trim();
  const results = {
    fields: match(entries.fields, query),
    schemas: match(entries.schemas, query),
    // the index is too big to list without a query
    index: query ? match(entries.index, query) : [],
    nomenclatures: match(entries.nomenclatures, query),
  };

  const toggleGlobalSearch = useCallback(
    () => setOpen(!useSchemaStore.getState().searchOpen),
    [],
  );

  const handleSchemaOnSelect = useCallback((schemaName: string) => {
    setOpen(false);
    navigate({
      to: "/$schemaName",
      params: { schemaName },
    });
  }, []);

  const handleNomenclatureOnSelect = useCallback((nomenclatureName: string) => {
    setOpen(false);
    openNomenclatureDrawer(nomenclatureName);
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

  // index entries name schemas by label
  const handleIndexSelect = (label: string, fieldPath: string) => {
    const schemaName = schemas.find((s) => s.label === label)?.schemaName;
    if (!schemaName) return;
    // same page: the schema detail won't remount, so reveal like a local field
    if (schemaName === currentSchemaMatch?.params.schemaName) {
      handleFieldSelect(fieldPath);
      return;
    }
    setOpen(false);
    navigate({
      to: "/$schemaName",
      params: { schemaName },
      hash: fieldPath,
    });
  };

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
        <Command shouldFilter={false}>
          <CommandInput
            placeholder="Rechercher..."
            value={search}
            onValueChange={setSearch}
          />
          <CommandList>
            <CommandEmpty>Aucun résultat trouvé.</CommandEmpty>
            {results.fields.length > 0 && (
              <CommandGroup heading={`Champs du ${currentSchemaLabel}`}>
                {results.fields.map(({ value, item: { fieldPath, title } }) => (
                  <CommandItem
                    key={value}
                    value={value}
                    onSelect={() => handleFieldSelect(fieldPath)}
                  >
                    <div className="flex min-w-0 flex-col">
                      <span>
                        <Highlight text={fieldPath} query={query} />
                      </span>
                      {title && (
                        <span className="truncate text-xs text-muted-foreground">
                          <Highlight text={title} query={query} />
                        </span>
                      )}
                    </div>
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {results.schemas.length > 0 && (
              <CommandGroup heading="Schemas">
                {results.schemas.map(({ value, item: s }) => (
                  <CommandItem
                    key={s.schemaName}
                    value={value}
                    onSelect={() => handleSchemaOnSelect(s.schemaName)}
                  >
                    <Braces className="opacity-60 h-3 w-3" />
                    <Highlight text={s.label} query={query} />
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {results.index.length > 0 && (
              <CommandGroup heading="Autres schemas">
                {results.index.map(({ value, item: s }) => (
                  <CommandItem
                    key={value}
                    value={value}
                    onSelect={() => handleIndexSelect(s.schema, s.path)}
                  >
                    <div className="flex min-w-0 flex-col gap-1">
                      <div className="flex min-w-0 items-center gap-2">
                        <Badge
                          variant={"perimeter"}
                          className="text-[10px] px-1"
                        >
                          <Highlight text={s.schema} query={query} />
                        </Badge>
                        <span className="truncate">
                          <Highlight text={s.path} query={query} />
                        </span>
                      </div>
                      {s.schemaTitle && (
                        <span className="truncate text-xs text-muted-foreground">
                          <Highlight text={s.schemaTitle} query={query} />
                        </span>
                      )}
                    </div>
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {results.nomenclatures.length > 0 && (
              <CommandGroup heading="Nomenclatures">
                {results.nomenclatures.map(({ value, item: n }) => (
                  <CommandItem
                    key={n.schemaName}
                    value={value}
                    onSelect={() => handleNomenclatureOnSelect(n.label)}
                  >
                    <Type className="opacity-60 h-3 w-3" />
                    {n.label}
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
          </CommandList>
        </Command>
      </CommandDialog>
    </>
  );
};
export default GlobalSearch;
