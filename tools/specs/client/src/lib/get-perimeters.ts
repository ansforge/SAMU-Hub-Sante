import { SchemaReference } from "@/types";

export const getPerimeters = (schemas: SchemaReference[]) => {
  return [...new Set(schemas.flatMap((schema) => schema?.perimeters || []))];
};
