import type { JsonSchemaProperty } from "@/types";
import { FieldValues } from "./field-values";
import { useState } from "react";

const PREVIEW_LENGTH = 140;

export function FieldDescription({ prop }: { prop: JsonSchemaProperty }) {
  const [detailed, setDetailed] = useState(false);

  if (!prop.description && !prop.enum) return null;
  const description = prop.description ?? "";
  const long = description.length > PREVIEW_LENGTH;
  const cut = description.lastIndexOf(" ", PREVIEW_LENGTH);
  const text =
    long && !detailed
      ? `${description.slice(0, cut > 0 ? cut : PREVIEW_LENGTH)}… `
      : `${description} `;

  return (
    <div className="space-y-1.5 text-left">
      {prop.description && (
        <p className="text-sm font-normal text-muted-foreground">
          {text}
          {long && (
            <button
              className="text-primary underline"
              onClick={() => setDetailed(!detailed)}
            >
              {detailed ? "moins" : "plus"}
            </button>
          )}
        </p>
      )}
      {prop.enum && <FieldValues values={prop.enum} />}
    </div>
  );
}
