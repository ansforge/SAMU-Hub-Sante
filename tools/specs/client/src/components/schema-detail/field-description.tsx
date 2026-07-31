import type { JsonSchemaProperty } from "@/types";

export function FieldDescription({ prop }: { prop: JsonSchemaProperty }) {
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
