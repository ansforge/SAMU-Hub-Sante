import { cn } from "@/lib/utils";
import { useSchemaStore } from "@/store/schema-store";

export function MessageList() {
  const schemas = useSchemaStore((s) => s.schemas);
  const selectedName = useSchemaStore((s) => s.selectedName);
  const selectSchema = useSchemaStore((s) => s.selectSchema);

  return (
    <div className="flex overflow-x-auto border-y divide-x divide-x-border">
      {Object.values(schemas).map((schema) => (
        <button
          key={schema.name}
          onClick={() => selectSchema(schema.name)}
          className={cn(
            "shrink-0 px-4 py-2 text-sm font-medium",
            schema.name === selectedName
              ? "bg-background"
              : "bg-muted text-muted-foreground hover:bg-muted/70"
          )}
        >
          {schema.name}
        </button>
      ))}
    </div>
  );
}
