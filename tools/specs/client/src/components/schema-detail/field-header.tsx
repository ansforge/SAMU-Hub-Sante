import {
  NomenclatureBadge,
} from "@/components/nomenclature-drawer";
import type {
  JsonSchemaDefinitions,
  JsonSchemaProperty,
} from "@/types";
import { fieldCardinality, fieldType } from "./schema-utils";

export function FieldHeader({
  name,
  prop,
  required,
  definitions,
}: {
  name: string;
  prop: JsonSchemaProperty;
  required: boolean;
  definitions: JsonSchemaDefinitions;
}) {
  return (
    <div className="flex flex-1 flex-wrap items-center gap-2 pr-2 text-left">
      <span className="font-mono text-sm">
        {name}
        {required && <span className="text-destructive"> *</span>}

      </span>
      <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
        {fieldType(prop, definitions)}
      </span>
      <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
        {fieldCardinality(prop, definitions, required)}
      </span>
      {prop["x-nomenclature"] && (
        <NomenclatureBadge name={prop["x-nomenclature"]} />
      )}
    </div>
  );
}
