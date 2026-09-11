import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

// short enum lists read fine as inline badges; longer ones would wrap into a
// wall of chips, so they collapse behind a "voir les valeurs" popover
const INLINE_THRESHOLD = 1;

function ValueBadge({ value }: { value: string | number }) {
  return (
    <span className="rounded-full bg-muted px-2 py-0.5 font-mono text-[11px] text-foreground">
      {value}
    </span>
  );
}

export function FieldValues({ values }: { values: (string | number)[] }) {
  if (!values.length) return null;

  return (
    <div className="flex flex-wrap items-center gap-1.5 text-xs">
      {values.length <= INLINE_THRESHOLD ? (
        values.map((value) => <ValueBadge key={value} value={value} />)
      ) : (
        <Popover>
          <PopoverTrigger className="rounded-full border border-border px-2 py-0.5 text-[11px] text-foreground hover:bg-muted">
            <span>{values.length} Valeurs possibles</span>
          </PopoverTrigger>
          <PopoverContent className="flex w-auto max-w-sm flex-row flex-wrap gap-1.5">
            {values.map((value) => (
              <ValueBadge key={value} value={value} />
            ))}
          </PopoverContent>
        </Popover>
      )}
    </div>
  );
}
