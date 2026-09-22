import { cn } from "@/lib/utils";
import type { JsonSchemaDefinitions, JsonSchemaProperty } from "@/types";
import { fieldKind, fieldType, type FieldKind } from "./schema-utils";
import { CopyButton } from "../copy-button";

export const KIND_BADGE: Record<FieldKind, string> = {
  object: "bg-violet-100 text-violet-700",
  array: "bg-emerald-100 text-emerald-700",
  simple: "bg-muted text-muted-foreground",
};

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
  path?: string[];
}) {
  const kind = fieldKind(prop, definitions);

  return (
    <div className="flex flex-col items-start gap-0.5 text-left">
      {path && path.length > 1 && (
        <span className="group relative flex items-center gap-1 h-6 font-mono text-[11px] text-muted-foreground/90">
          {path.join(".")}
          <CopyButton content={path.join(".")} />
        </span>
      )}
      <div className="flex flex-wrap items-baseline gap-2">
        <span className="font-mono text-[15px] leading-none font-semibold text-primary">
          {name}
          {required && <span className="ml-0.5 text-destructive">*</span>}
        </span>
        <span
          className={cn(
            "rounded-full px-2 py-0.5 font-mono text-[11px] font-medium",
            KIND_BADGE[kind],
          )}
        >
          {badgeLabel(kind, prop, definitions)}
        </span>
      </div>
      {prop.title && (
        <span className="text-sm font-medium text-foreground/80">
          {prop.title}
        </span>
      )}
    </div>
  );
}
