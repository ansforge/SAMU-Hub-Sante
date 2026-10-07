import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { NomenclatureDrawer } from "@/components/nomenclature-drawer";
import type { JsonSchemaDocument } from "@/types";
import { FieldLegend } from "./field-legend";
import { SchemaFields } from "./schema-fields";
import { buildGithubSchemaUrl } from "@/lib/utils";
import { githubDomain } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import { ExternalLink } from "../external-link";
import { SchemaBadges } from "./schema-badges";

type SchemaDetailProps = {
  schema: JsonSchemaDocument;
  label: string;
  perimeters: string[];
  schemaName: string;
  ref: string;
};

export function SchemaDetail({
  schema,
  label,
  perimeters,
  schemaName,
  ref,
}: SchemaDetailProps) {
  const expandSignal = useSchemaStore((s) => s.expandSignal);
  const toggleExpandAll = useSchemaStore((s) => s.toggleExpandAll);

  const properties = schema.properties ?? {};
  const hasProperties = Object.keys(properties).length > 0;
  const definitions = schema.definitions ?? schema.$defs ?? {};

  const schemaSource = buildGithubSchemaUrl(
    githubDomain,
    `blob/${ref}`,
    schemaName,
  );

  return (
    <>
      <NomenclatureDrawer />
      <div className="mx-auto flex min-h-0 w-full max-w-7xl flex-1 flex-col gap-6 overflow-y-auto px-4 py-6 md:px-8">
        <header className="flex flex-col gap-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold">{label}</h1>
            <SchemaBadges perimeters={perimeters} />
            <ExternalLink href={schemaSource} className="ml-auto">
              Voir sur GitHub
            </ExternalLink>
          </div>
          <h2 className="text-lg font-medium">{schema.title}</h2>
          {schema.description && (
            <p className="max-w-prose text-sm text-muted-foreground">
              {schema.description}
            </p>
          )}
        </header>

        {hasProperties && (
          <section className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <FieldLegend />
              <Button variant="outline" size="sm" onClick={toggleExpandAll}>
                {expandSignal.expand ? "Tout replier" : "Tout déplier"}
              </Button>
            </div>

            <SchemaFields
              properties={properties}
              required={schema.required}
              definitions={definitions}
              expandSignal={expandSignal}
            />
          </section>
        )}
      </div>
    </>
  );
}

export function SchemaDetailSkeleton() {
  return (
    <div className="mx-auto flex min-h-0 w-full max-w-7xl flex-1 flex-col gap-6 overflow-y-auto px-4 py-6 md:px-8">
      <div className="flex flex-col gap-1">
        <Skeleton className="h-7 w-48" />
        <Skeleton className="h-5 w-80 max-w-full" />
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-3">
          <Skeleton className="h-6 w-72 max-w-full" />
          <Skeleton className="h-8 w-24 shrink-0" />
        </div>
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
          ))}
        </div>
      </div>
    </div>
  );
}
