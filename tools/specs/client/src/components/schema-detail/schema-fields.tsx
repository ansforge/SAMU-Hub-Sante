import { Accordion as AccordionPrimitive } from "@base-ui/react/accordion";
import { ChevronDownIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import type { JsonSchemaDefinitions, JsonSchemaProperty } from "@/types";
import { FieldDescription } from "./field-description";
import { FieldHeader } from "./field-header";
import { FieldMeta } from "./field-meta";
import { nestedFields, type ExpandSignal } from "./schema-utils";

// nested levels are built from the bare base-ui primitives (not the styled
// ui/accordion.tsx wrapper): that wrapper's default border/bg only cancel
// cleanly at the top level — fighting its data-open:border-border rule with
// an override class loses the specificity/ordering battle deeper in the tree
export function SchemaFields({
  properties,
  required,
  definitions,
  expandSignal,
  path = [],
}: {
  properties: Record<string, JsonSchemaProperty>;
  required?: string[];
  definitions: JsonSchemaDefinitions;
  expandSignal: ExpandSignal;
  path?: string[];
}) {
  const requiredNames = new Set(required ?? []);
  const names = Object.keys(properties);
  const depth = path.length;

  return (
    <AccordionPrimitive.Root
      key={expandSignal.key}
      multiple
      defaultValue={expandSignal.expand ? names : []}
      className={cn(
        "flex w-full flex-col",
        depth === 0 ? "gap-2" : "divide-y divide-border/60",
      )}
    >
      {Object.entries(properties).map(([name, prop]) => {
        const nested = nestedFields(prop, definitions);
        const isRequired = requiredNames.has(name);
        const fieldPath = [...path, name];

        const row = (
          <div className="flex flex-col gap-1 py-2.5">
            <FieldHeader
              definitions={definitions}
              name={name}
              prop={prop}
              required={isRequired}
              path={fieldPath}
            />
            <FieldDescription prop={prop} />
            <FieldMeta prop={prop} definitions={definitions} />
          </div>
        );

        if (!nested) {
          return (
            <div
              key={name}
              className={cn(depth === 0 && "border-b border-border/60 px-1")}
            >
              {row}
            </div>
          );
        }

        return (
          <AccordionPrimitive.Item
            key={name}
            value={name}
            className={cn(
              depth === 0 &&
                "rounded-lg border border-border/70 bg-card/40 px-3",
            )}
          >
            <AccordionPrimitive.Header className="flex">
              <AccordionPrimitive.Trigger className="group/trigger flex flex-1 items-start justify-between gap-2 rounded-md px-1 text-left outline-none hover:bg-muted/40 focus-visible:ring-2 focus-visible:ring-ring/50">
                {row}
                <ChevronDownIcon className="mt-3 size-3.5 shrink-0 text-muted-foreground/50 transition-transform duration-150 group-aria-expanded/trigger:rotate-180" />
              </AccordionPrimitive.Trigger>
            </AccordionPrimitive.Header>
            <AccordionPrimitive.Panel className="overflow-hidden text-sm data-open:animate-accordion-down data-closed:animate-accordion-up">
              <div className="h-(--accordion-panel-height) data-ending-style:h-0 data-starting-style:h-0">
                <div
                  className={cn(
                    "ml-3 border-l border-border/50 pl-4",
                    depth === 0 ? "mt-1 mb-2" : "mb-1",
                  )}
                >
                  <SchemaFields
                    properties={nested.properties}
                    required={nested.required}
                    definitions={definitions}
                    expandSignal={expandSignal}
                    path={fieldPath}
                  />
                </div>
              </div>
            </AccordionPrimitive.Panel>
          </AccordionPrimitive.Item>
        );
      })}
    </AccordionPrimitive.Root>
  );
}
