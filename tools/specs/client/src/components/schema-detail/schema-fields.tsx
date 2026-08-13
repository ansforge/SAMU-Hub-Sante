import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import type {
  JsonSchemaDefinitions,
  JsonSchemaProperty,
} from "@/types";
import { FieldDescription } from "./field-description";
import { FieldHeader } from "./field-header";
import { nestedFields, type ExpandSignal } from "./schema-utils";

export function SchemaFields({
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
