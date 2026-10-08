import { cn } from "@/lib/utils";

interface SchemaBadges {
  perimeters: string[];
}
export const SchemaBadges = ({ perimeters }: SchemaBadges) => {
  return (
    <div className="flex items-center gap-1">
      {perimeters.map((p) => (
        <span
          key={p}
          className={cn(
            "rounded-full px-2 py-0.5 font-mono text-[11px] font-medium",
            "bg-sky-100 text-sky-700",
          )}
        >
          {p}
        </span>
      ))}
    </div>
  );
};
