import type { JsonSchemaDefinitions, JsonSchemaProperty } from "@/types";
import { NomenclatureBadge } from "../nomenclature-drawer";
import { fieldOccurrences, resolveRef } from "./schema-utils";

export function FieldMeta({
  prop,
  definitions,
}: {
  prop: JsonSchemaProperty;
  definitions: JsonSchemaDefinitions;
}) {
  const resolved = resolveRef(prop, definitions);
  const isArray = resolved.type === "array";
  const hasNomenclature = Boolean(prop[NOMENCLATURE_KEY]);

  if (!resolved.format && !isArray && !hasNomenclature) return null;

  return (
    <div className="flex flex-col gap-1 text-xs text-muted-foreground">
      {(resolved.format || isArray) && (
        <div className="flex flex-wrap items-center gap-x-3">
          {resolved.format && (
            <span>
              Format : <span className="font-medium">{resolved.format}</span>
            </span>
          )}
          {isArray && (
            <span>{fieldOccurrences(prop, definitions)} élément(s)</span>
          )}
        </div>
      )}
      {hasNomenclature && (
        <NomenclatureBadge name={prop[NOMENCLATURE_KEY] as string} />
      )}
    </div>
  );
}
