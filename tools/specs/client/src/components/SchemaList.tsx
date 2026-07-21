import { cn } from "@/lib/utils";
import { useSchemaStore } from "@/store/schema-store";
import { Link } from "@tanstack/react-router";

export function SchemaList() {
  const schemas = useSchemaStore((s) => s.schemas);
  const selectedName = useSchemaStore((s) => s.selectedName);

  return (
    <div className="flex overflow-x-auto border-y divide-x divide-x-border">
      {Object.values(schemas).map((schema) => (
        <Link
          key={schema.name}
          to="/$schemaName"
          params={{ schemaName: schema.name }}
          className={cn(
            "shrink-0 px-4 py-2 text-sm font-medium",
            schema.name === selectedName
              ? "bg-background"
              : "bg-muted text-muted-foreground hover:bg-muted/70",
          )}
        >
          {schema.name}
        </Link>
      ))}
    </div>
  );
}
