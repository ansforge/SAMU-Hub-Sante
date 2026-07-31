import type {
  JsonSchemaDefinitions,
  JsonSchemaProperty,
} from "@/types";

export type ExpandSignal = {
  // bumped on every "expand/collapse all" click, forcing every Accordion in
  // the tree to remount with the new defaultValue instead of staying
  // uncontrolled-but-stuck on its old state
  key: number;
  expand: boolean;
};

export function refName(ref: string): string {
  return ref.split("/").pop() ?? ref;
}

// definitions are referenced with $ref instead of inlined properties, so
// dereference (following chained refs) before reading type/properties/items
export function resolveRef(
  prop: JsonSchemaProperty,
  definitions: JsonSchemaDefinitions,
  seen = new Set<string>(),
): JsonSchemaProperty {
  if (!prop.$ref) return prop;
  const name = refName(prop.$ref);
  const resolved = definitions[name];
  if (!resolved || seen.has(name)) return prop;
  return resolveRef(resolved, definitions, new Set(seen).add(name));
}

export function fieldType(
  prop: JsonSchemaProperty,
  definitions: JsonSchemaDefinitions,
): string {
  const resolved = resolveRef(prop, definitions);

  if (resolved.type === "array") {
    const itemType = resolved.items
      ? fieldType(resolved.items, definitions)
      : "?";
    return `array<${itemType}>`;
  }
  if (prop.$ref && resolved.properties) {
    return refName(prop.$ref);
  }
  return resolved.type ?? "—";
}

// array fields: minItems/maxItems live on the array node itself (not on
// `items`). non-array fields: occurrence is 0/1, driven by `required`.
export function fieldCardinality(
  prop: JsonSchemaProperty,
  definitions: JsonSchemaDefinitions,
  required: boolean,
): string {
  const resolved = resolveRef(prop, definitions);
  if (resolved.type === "array") {
    const min = resolved.minItems ?? 0;
    const max = resolved.maxItems ?? "*";
    return `min ${min} · max ${max}`;
  }
  return `min ${required ? 1 : 0} · max 1`;
}

// object fields nest under `properties`, arrays of objects under `items`,
// both possibly reached through a $ref into the schema's definitions
export function nestedFields(
  prop: JsonSchemaProperty,
  definitions: JsonSchemaDefinitions,
): {
  properties: Record<string, JsonSchemaProperty>;
  required?: string[];
} | null {
  const resolved = resolveRef(prop, definitions);
  const target =
    resolved.type === "array"
      ? resolved.items && resolveRef(resolved.items, definitions)
      : resolved;

  if (!target?.properties) return null;
  return { properties: target.properties, required: target.required };
}
