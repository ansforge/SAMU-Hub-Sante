import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { NomenclatureDrawer } from "@/components/nomenclature-drawer";
import type { JsonSchemaDocument } from "@/types";
import { FieldLegend } from "./field-legend";
import { SchemaFields } from "./schema-fields";
import type { ExpandSignal } from "./schema-utils";

type SchemaDetailProps = {
  schema?: JsonSchemaDocument;
};

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
    <>
      <NomenclatureDrawer />
      <div className="min-h-0 flex-1 overflow-y-auto p-8 w-full max-w-7xl mx-auto">
        <div>
          <h1 className="text-lg font-semibold">{schema.title}</h1>
          {schema.description && (
            <p className="mt-1 text-sm text-muted-foreground">
              {schema.description}
            </p>
          )}
        </div>

        {hasProperties && (
          <div className="mt-8">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h2 className="text-base font-semibold">
                  Structure de l'objet
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Cliquez sur un élément pour voir le détail.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <FieldLegend />
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setExpandSignal((s) => ({
                      key: s.key + 1,
                      expand: !s.expand,
                    }))
                  }
                >
                  {expandSignal.expand ? "Tout replier" : "Tout déplier"}
                </Button>
              </div>
            </div>

            <div className="mt-6">
              <SchemaFields
                properties={properties}
                required={schema.required}
                definitions={definitions}
                expandSignal={expandSignal}
              />
            </div>
          </div>
        )}
      </div>
    </>
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
