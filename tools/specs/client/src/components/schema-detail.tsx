import { useState } from "react";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import type {
  JsonSchemaDefinitions,
  JsonSchemaDocument,
  JsonSchemaProperty,
} from "@/types";

type SchemaDetailProps = {
  schema?: JsonSchemaDocument;
};

function refName(ref: string): string {
  return ref.split("/").pop() ?? ref;
}

// definitions are referenced with $ref instead of inlined properties, so
// dereference (following chained refs) before reading type/properties/items
function resolveRef(
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

function fieldType(
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

// object fields nest under `properties`, arrays of objects under `items`,
// both possibly reached through a $ref into the schema's definitions
function nestedFields(
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

function FieldHeader({
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
      <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-muted-foreground">
        {fieldType(prop, definitions)}
      </span>
    </div>
  );
}

function FieldDescription({ prop }: { prop: JsonSchemaProperty }) {
  if (!prop.description && !prop.enum) return null;
  return (
    <div className="text-sm text-muted-foreground">
      {prop.description}
      {prop.enum && (
        <div className="mt-0.5 text-xs">enum : {prop.enum.join(", ")}</div>
      )}
    </div>
  );
}

type ExpandSignal = {
  // bumped on every "expand/collapse all" click, forcing every Accordion in
  // the tree to remount with the new defaultValue instead of staying
  // uncontrolled-but-stuck on its old state
  key: number;
  expand: boolean;
};

function SchemaFields({
  properties,
  required,
  definitions,
  expandSignal,
}: {
  properties: Record<string, JsonSchemaProperty>;
  required?: string[];
  definitions: JsonSchemaDefinitions;
  expandSignal: ExpandSignal;
}) {
  const requiredNames = new Set(required ?? []);
  const names = Object.keys(properties);

  return (
    <Accordion
      key={expandSignal.key}
      multiple
      defaultValue={expandSignal.expand ? names : []}
    >
      {Object.entries(properties).map(([name, prop]) => {
        const nested = nestedFields(prop, definitions);
        const isRequired = requiredNames.has(name);

        if (!nested) {
          return (
            <div key={name} className="px-3 py-2.5 mt-2">
              <FieldHeader
                name={name}
                prop={prop}
                required={isRequired}
                definitions={definitions}
              />
              <FieldDescription prop={prop} />
            </div>
          );
        }

        return (
          <AccordionItem key={name} value={name} className={"my-1"}>
            <AccordionTrigger>
              <FieldHeader
                name={name}
                prop={prop}
                required={isRequired}
                definitions={definitions}
              />
            </AccordionTrigger>
            <AccordionContent>
              <FieldDescription prop={prop} />
              <div className="mt-2 pl-4">
                <SchemaFields
                  properties={nested.properties}
                  required={nested.required}
                  definitions={definitions}
                  expandSignal={expandSignal}
                />
              </div>
            </AccordionContent>
          </AccordionItem>
        );
      })}
    </Accordion>
  );
}

export function SchemaDetail({ schema }: SchemaDetailProps) {
  const [expandSignal, setExpandSignal] = useState<ExpandSignal>({
    key: 0,
    expand: false,
  });

  if (!schema) {
    return (
      <div className="flex flex-1 items-center justify-center text-muted-foreground">
        Sélectionnez un schéma pour voir ses détails.
      </div>
    );
  }

  const properties = schema.properties ?? {};
  const hasProperties = Object.keys(properties).length > 0;
  const definitions = schema.definitions ?? schema.$defs ?? {};

  return (
    <div className="min-h-0 flex-1 overflow-y-auto p-8 w-full max-w-7xl mx-auto">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-semibold">{schema.title}</h1>
          {schema.description && (
            <p className="mt-1 text-sm text-muted-foreground">
              {schema.description}
            </p>
          )}
        </div>
        {hasProperties && (
          <Button
            variant="outline"
            onClick={() =>
              setExpandSignal((s) => ({ key: s.key + 1, expand: !s.expand }))
            }
          >
            {expandSignal.expand ? "Tout replier" : "Tout déplier"}
          </Button>
        )}
      </div>

      {hasProperties && (
        <div className="mt-6">
          <SchemaFields
            properties={properties}
            required={schema.required}
            definitions={definitions}
            expandSignal={expandSignal}
          />
        </div>
      )}
    </div>
  );
}

export function SchemaDetailSkeleton() {
  return (
    <div className="min-h-0 flex-1 overflow-y-auto p-8 w-full max-w-7xl mx-auto">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-5 w-48" />
          <Skeleton className="h-4 w-80" />
        </div>
        <Skeleton className="h-8 w-24 shrink-0" />
      </div>

      <div className="mt-6 space-y-1">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-11 w-full" />
        ))}
      </div>
    </div>
  );
}
