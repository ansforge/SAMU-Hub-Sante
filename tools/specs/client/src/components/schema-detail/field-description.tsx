import type { JsonSchemaProperty } from "@/types";
import { FieldValues } from "./field-values";

export function FieldDescription({ prop }: { prop: JsonSchemaProperty }) {
  if (!prop.description && !prop.enum) return null;
  return (
    <div className="space-y-1.5 text-left">
      {prop.description && (
        <p className="text-sm text-muted-foreground">{prop.description}</p>
      )}
      {prop.enum && <FieldValues values={prop.enum} />}
    </div>
  );
}
