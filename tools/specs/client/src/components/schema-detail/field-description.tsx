import type { JsonSchemaProperty } from "@/types";
import { FieldValues } from "./field-values";
import { useState } from "react";
import { cn } from "@/lib/utils";

export function FieldDescription({ prop }: { prop: JsonSchemaProperty }) {
  const [detailed, setDetailed] = useState(false);
  const [overflows, setOverflows] = useState(false);

  if (!prop.description && !prop.enum) return null;
  return (
    <div className="space-y-1.5 text-left">
      {prop.description && (
        <div className="flex items-baseline gap-2 text-sm">
          <p
            // ponytail: measured on render only, add a ResizeObserver if resizes matter
            ref={(el) => {
              if (el && !detailed) setOverflows(el.scrollHeight > el.clientHeight);
            }}
            className={cn("flex-1 text-muted-foreground", !detailed && "line-clamp-1")}
          >
            {prop.description}
          </p>
          {overflows && (
            <button className="text-primary underline" onClick={() => setDetailed(!detailed)}>
              {detailed ? "Moins" : "Plus"}
            </button>
          )}
        </div>
      )}
      {prop.enum && <FieldValues values={prop.enum} />}
    </div>
  );
}
