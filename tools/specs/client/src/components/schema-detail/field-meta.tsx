import type { JsonSchemaDefinitions, JsonSchemaProperty } from "@/types";
import { NomenclatureBadge } from "../nomenclature-drawer";
import { fieldCardinality, resolveRef } from "./schema-utils";
import { NOMENCLATURE_KEY } from "@/config";

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
  const hasPattern = Boolean(resolved["pattern"]);
  // objects carry their example on the definition as a whole subtree, too big
  // to inline here; only leaf values are shown
  const example =
    typeof prop.example === "string" ||
    typeof prop.example === "number" ||
    typeof prop.example === "boolean"
      ? String(prop.example)
      : undefined;

  if (!resolved.format && !isArray && !hasNomenclature && example === undefined)
    return null;

  return (
    <div className="flex flex-col gap-1 text-xs text-muted-foreground">
      {example !== undefined && (
        <span>
          Exemple :{" "}
          <code className="rounded bg-muted px-1 py-0.5 font-mono text-[11px] text-foreground break-all">
            {example}
          </code>
        </span>
      )}
      {(resolved.format || isArray) && (
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          {resolved.format && (
            <span className="border rounded px-1">
              Format : <span className="font-medium">{resolved.format}</span>
            </span>
          )}
          {hasPattern && (
            <span className="border rounded px-1">
              Regex : <span className="font-medium">{resolved["pattern"]}</span>
            </span>
          )}
          {isArray && (
            <span>{fieldCardinality(prop, definitions)} élément(s)</span>
          )}
        </div>
      )}
      {hasNomenclature && (
        <NomenclatureBadge name={prop[NOMENCLATURE_KEY] as string} />
      )}
    </div>
  );
}
