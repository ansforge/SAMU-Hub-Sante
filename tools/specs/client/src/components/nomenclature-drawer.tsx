import { Input } from "@/components/ui/input";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { githubDomain } from "@/config";
import { useNomenclature } from "@/hooks/use-nomenclature";
import { buildNomenclatureUrl } from "@/lib/utils";
import { useSchemaStore } from "@/store/schema-store";
import { getRouteApi } from "@tanstack/react-router";
import { ExternalLinkIcon } from "lucide-react";
import { useMemo, useState } from "react";
import SourceLink from "./source-link";

const rootRouteApi = getRouteApi("__root__");

function NomenclatureHeader({ name }: { name: string }) {
  const { data, isPending } = useNomenclature(name);
  const { ref } = rootRouteApi.useSearch();

  if (isPending) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-4 w-64" />
      </div>
    );
  }

  const nomenclatureSource = buildNomenclatureUrl(
    githubDomain,
    `blob/${ref}`,
    name,
  );

  return (
    <>
      <div className="flex items-center gap-2">
        <SheetTitle className="font-bold text-xl">
          {data?.title ?? name}
        </SheetTitle>
        <SourceLink href={nomenclatureSource} />
      </div>
      {data?.description && (
        <p className="text-sm text-muted-foreground">{data.description}</p>
      )}
    </>
  );
}

function NomenclatureContent({ name }: { name: string }) {
  const { data, isPending, isError } = useNomenclature(name);
  const [search, setSearch] = useState<string>("");

  const filteredOptions = useMemo(() => {
    const q = search.toLocaleLowerCase();
    return Object.values(data?.oneOf ?? {}).filter(
      (s) =>
        s.title.toLowerCase().includes(q) ||
        String(s.const).toLowerCase().includes(q) ||
        (s.description ?? "").toLowerCase().includes(q),
    );
  }, [search, data]);

  if (isPending) {
    return (
      <div className="space-y-2 p-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-6 w-full" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <p className="p-4 text-sm text-muted-foreground">
        Impossible de charger la nomenclature.
      </p>
    );
  }

  return (
    <>
      <Input
        autoFocus
        type="search"
        placeholder="Rechercher..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="m-4 w-[calc(100%-2rem)]"
      />
      <table className="w-full text-sm">
        <tbody>
          {filteredOptions?.map((option) => (
            <tr key={option.const} className="border-b last:border-0">
              <td className="w-px whitespace-nowrap p-4 align-top">
                <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-muted-foreground">
                  {option.const}
                </span>
              </td>
              <td className="p-4 pl-0 align-top">
                <p className="font-bold text-sm">{option.title}</p>
                <p className="italic text-xs">{option?.description}</p>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

export function NomenclatureDrawer() {
  const name = useSchemaStore((s) => s.nomenclatureDrawerName);
  const closeNomenclatureDrawer = useSchemaStore(
    (s) => s.closeNomenclatureDrawer,
  );

  return (
    <Sheet
      open={name !== null}
      onOpenChange={(open) => !open && closeNomenclatureDrawer()}
    >
      <SheetContent className={"w-full max-w-4xl!"}>
        <SheetHeader className="border-b">
          {name && <NomenclatureHeader name={name} />}
        </SheetHeader>
        <div className="min-h-0 flex-1 overflow-y-auto w-full">
          {name && <NomenclatureContent name={name} />}
        </div>
      </SheetContent>
    </Sheet>
  );
}

export function NomenclatureBadge({ name }: { name: string }) {
  const openNomenclatureDrawer = useSchemaStore(
    (s) => s.openNomenclatureDrawer,
  );

  return (
    <span className="whitespace-nowrap font-mono text-xs text-muted-foreground">
      nomenclature :{" "}
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          openNomenclatureDrawer(name);
        }}
        className="cursor-pointer text-primary underline underline-offset-2 hover:text-primary/80"
      >
        {name}
      </button>
    </span>
  );
}
