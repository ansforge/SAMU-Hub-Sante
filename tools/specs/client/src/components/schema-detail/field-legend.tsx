import { KIND_BADGE, PRIMITIVE_BADGE } from "./field-header";

const LEGEND: {
  label: string;
  className: string;
  asterisk?: boolean;
  mono?: boolean;
}[] = [
  {
    label: "Requis",
    className: "border border-border text-foreground",
    asterisk: true,
  },
  { label: "Objet", className: KIND_BADGE.object },
  { label: "Collection", className: KIND_BADGE.array },
  { label: "string", className: PRIMITIVE_BADGE.string, mono: true },
  { label: "number", className: PRIMITIVE_BADGE.number, mono: true },
  { label: "boolean", className: PRIMITIVE_BADGE.boolean, mono: true },
];

export function FieldLegend() {
  return (
    <div className="flex flex-wrap items-center gap-2 text-xs font-medium">
      {LEGEND.map(({ label, className, asterisk, mono }) => (
        <span
          key={label}
          className={`rounded-full px-3 py-1 ${mono ? "font-mono" : ""} ${className}`}
        >
          {asterisk && <span className="mr-1 text-destructive">*</span>}
          {label}
        </span>
      ))}
    </div>
  );
}
