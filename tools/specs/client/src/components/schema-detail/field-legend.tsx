import { Badge, type BadgeVariant } from "@/components/ui/badge";

const LEGEND: { label: string; variant: BadgeVariant }[] = [
  { label: "Objet", variant: "object" },
  { label: "Collection", variant: "array" },
  { label: "string", variant: "string" },
  { label: "number", variant: "number" },
  { label: "boolean", variant: "boolean" },
];

export function FieldLegend() {
  return (
    <div className="flex flex-wrap items-center gap-2">
      {LEGEND.map(({ label, variant }) => (
        <Badge key={label} variant={variant}>
          {label}
        </Badge>
      ))}
    </div>
  );
}
