import { useState } from "react";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import type { JsonSchemaDefinitions, JsonSchemaDocument, JsonSchemaProperty } from "@/types";

type SchemaDetailProps = {
  schema?: JsonSchemaDocument;
};

export function SchemaDetail({ schema }: SchemaDetailProps) {
  if (!schema) {
    return (
      <div className="flex flex-1 items-center justify-center text-muted-foreground">
        Sélectionnez un schéma pour voir ses détails.
      </div>
    );
  }

  const properties = Object.entries(schema.properties ?? {});
  const required = new Set(schema.required ?? []);

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <h1 className="text-lg font-semibold">{schema.title}</h1>
      {schema.description && (
        <p className="mt-1 text-sm text-muted-foreground">{schema.description}</p>
      )}

      {properties.length > 0 && (
        <table className="mt-6 w-full text-sm">
          <thead>
            <tr className="border-b text-left text-muted-foreground">
              <th className="py-2 pr-4 font-medium">Champ</th>
              <th className="py-2 pr-4 font-medium">Type</th>
              <th className="py-2 font-medium">Description</th>
            </tr>
          </thead>
          <tbody>
            {properties.map(([name, prop]) => (
              <tr key={name} className="border-b last:border-0">
                <td className="py-2 pr-4 font-mono">
                  {name}
                  {required.has(name) && <span className="text-destructive"> *</span>}
                </td>
                <td className="py-2 pr-4 text-muted-foreground">{prop.type ?? "—"}</td>
                <td className="py-2 text-muted-foreground">{prop.description ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
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
