import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { useNomenclature } from "@/hooks/use-nomenclature";
import { useSchemaStore } from "@/store/schema-store";

function NomenclatureHeader({ name }: { name: string }) {
  const { data, isPending } = useNomenclature(name);

  if (isPending) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-4 w-64" />
      </div>
    );
  }

  return (
    <>
      <SheetTitle className="font-bold text-xl">
        {data?.title ?? name}
      </SheetTitle>
      {data?.description && (
        <p className="text-sm text-muted-foreground">{data.description}</p>
      )}
    </>
  );
}

function NomenclatureContent({ name }: { name: string }) {
  const { data, isPending, isError } = useNomenclature(name);

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
    <table className="w-full text-sm">
      <tbody>
        {data.oneOf?.map((option) => (
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
