import { vhostPerimeters } from "@/config";
import { SchemaReference } from "@/types";

export const getPerimeters = (ref: string, schemas: SchemaReference[]) => {
  const fromVhosts = vhostPerimeters(ref);
  if (fromVhosts) return [...fromVhosts.keys()];
  return [...new Set(schemas.flatMap((schema) => schema?.perimeters || []))];
};
