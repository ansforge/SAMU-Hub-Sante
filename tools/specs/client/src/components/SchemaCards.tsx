import { Link } from "@tanstack/react-router";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useSchemaStore } from "@/store/schema-store";

export function SchemaCards() {
  const schemas = useSchemaStore((s) => s.schemas);

  return (
    <div className="grid grid-cols-1 gap-4 p-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {Object.values(schemas).map((schema) => (
        <Link key={schema.name} to="/$schemaName" params={{ schemaName: schema.name }}>
          <Card className="h-40 justify-center transition-colors hover:bg-muted/40 hover:ring-foreground/30">
            <CardHeader>
              <CardTitle className="font-mono">{schema.name}</CardTitle>
              <CardDescription className="truncate">{schema.path}</CardDescription>
            </CardHeader>
          </Card>
        </Link>
      ))}
    </div>
  );
}
