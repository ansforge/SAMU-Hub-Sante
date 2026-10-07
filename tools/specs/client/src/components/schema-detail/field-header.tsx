import { cn } from "@/lib/utils";
import { Badge, type BadgeVariant } from "@/components/ui/badge";
import type { JsonSchemaDefinitions, JsonSchemaProperty } from "@/types";
import { fieldKind, fieldType, type FieldKind } from "./schema-utils";
import { CopyButton } from "../copy-button";

const PRIMITIVE_VARIANT: Record<string, BadgeVariant> = {
  string: "string",
  number: "number",
  integer: "number",
  boolean: "boolean",
};

// primitives get one color per JSON type so string/number/boolean read apart;
// anything else falls back to its kind
export function fieldBadgeVariant(kind: FieldKind, label: string): BadgeVariant {
  return (kind === "simple" && PRIMITIVE_VARIANT[label]) || kind;
}

function badgeLabel(
  kind: FieldKind,
  prop: JsonSchemaProperty,
  definitions: JsonSchemaDefinitions,
): string {
  if (kind === "array") return "Collection";
  return fieldType(prop, definitions);
}

export function FieldHeader({
  name,
  prop,
  required,
  definitions,
  path,
}: {
  name: string;
  prop: JsonSchemaProperty;
  required: boolean;
  definitions: JsonSchemaDefinitions;
  path: string[];
}) {
  const kind = fieldKind(prop, definitions);
  const label = badgeLabel(kind, prop, definitions);
  const depth = path.length - 1;

  return (
    <div className="flex flex-col items-start gap-0.5 text-left">
      <div className="flex min-h-6 flex-wrap items-center gap-2">
        <span
          className={cn(
            "leading-none font-semibold text-primary",
            depth === 0 ? "text-base" : "text-sm",
          )}
        >
          {name}
        </span>
        <Badge variant={fieldBadgeVariant(kind, label)}>{label}</Badge>
        {required && (
          <span className="text-xs font-medium text-destructive">Requis</span>
        )}
        {depth > 0 && (
          <span className="group flex items-center gap-1 text-xs text-muted-foreground">
            {path.join(".")}
            <CopyButton content={path.join(".")} label="Copier le chemin" />
          </span>
        )}
      </div>
      {prop.title && (
        <span className="text-sm font-medium text-foreground">
          {prop.title}
        </span>
      )}
    </div>
  );
}
