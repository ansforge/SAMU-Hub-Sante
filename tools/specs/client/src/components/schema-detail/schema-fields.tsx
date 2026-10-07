import { ChevronRightIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { useSchemaStore } from "@/store/schema-store";
import type { JsonSchemaDefinitions, JsonSchemaProperty } from "@/types";
import { FieldDescription } from "./field-description";
import { FieldHeader } from "./field-header";
import { FieldMeta } from "./field-meta";
import { nestedFields } from "./schema-utils";

type Fields = {
  properties: Record<string, JsonSchemaProperty>;
  required?: string[];
  definitions: JsonSchemaDefinitions;
};

// outline-style tree: every level looks the same, depth reads from the
// indentation and the vertical guide each child list draws under its
// parent's chevron
export function SchemaFields({
  properties,
  required,
  definitions,
  path = [],
}: Fields & { path?: string[] }) {
  const requiredNames = new Set(required ?? []);

  return (
    <ul
      id={path.length ? `${path.join(".")}-children` : undefined}
      className={cn("flex flex-col", path.length > 0 && "ml-3 border-l pl-2")}
    >
      {Object.entries(properties).map(([name, prop]) => (
        <FieldNode
          key={name}
          name={name}
          prop={prop}
          required={requiredNames.has(name)}
          definitions={definitions}
          path={[...path, name]}
        />
      ))}
    </ul>
  );
}

function FieldNode({
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
  const id = path.join(".");
  const nested = nestedFields(prop, definitions);
  const open = useSchemaStore((s) => s.openFields.includes(id));
  const toggleField = useSchemaStore((s) => s.toggleField);

  return (
    <li id={id} className="scroll-mt-24">
      <div className="flex items-start gap-1 rounded-md py-1.5 pr-2 transition-colors duration-300 [:target>&]:bg-muted [:target>&]:ring-2 [:target>&]:ring-ring/40">
        {nested ? (
          <button
            type="button"
            aria-expanded={open}
            aria-controls={`${id}-children`}
            aria-label={`${open ? "Replier" : "Déplier"} ${name}`}
            onClick={() => toggleField(id)}
            className="flex size-6 shrink-0 cursor-pointer items-center justify-center rounded text-muted-foreground outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring/50"
          >
            <ChevronRightIcon
              className={cn(
                "size-4 transition-transform duration-150",
                open && "rotate-90",
              )}
            />
          </button>
        ) : (
          <span
            aria-hidden
            className="flex size-6 shrink-0 items-center justify-center"
          >
            <span className="size-1.5 rounded-full bg-muted-foreground/40" />
          </span>
        )}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <FieldHeader
            name={name}
            prop={prop}
            required={required}
            definitions={definitions}
            path={path}
          />
          <FieldDescription prop={prop} />
          <FieldMeta prop={prop} definitions={definitions} />
        </div>
      </div>
      {nested && open && (
        <SchemaFields
          properties={nested.properties}
          required={nested.required}
          definitions={definitions}
          path={path}
        />
      )}
    </li>
  );
}
