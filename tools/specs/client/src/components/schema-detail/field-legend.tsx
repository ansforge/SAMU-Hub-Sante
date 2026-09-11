import { KIND_BADGE } from "./field-header";

const LEGEND: { label: string; className: string; asterisk?: boolean }[] = [
  {
    label: "Requis",
    className: "border border-border text-foreground",
    asterisk: true,
  },
  { label: "Objet", className: KIND_BADGE.object },
  { label: "Collection", className: KIND_BADGE.array },
  { label: "Primitive", className: KIND_BADGE.simple },
];

export function FieldLegend() {
  return (
    <div className="flex flex-wrap items-center gap-2 text-xs font-medium">
      {LEGEND.map(({ label, className, asterisk }) => (
        <span key={label} className={`rounded-full px-3 py-1 ${className}`}>
          {asterisk && <span className="mr-1 text-destructive">*</span>}
          {label}
        </span>
      ))}
    </div>
  );
}
