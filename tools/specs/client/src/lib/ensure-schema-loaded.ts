import { apiDomain } from "@/config";
import { useSchemaStore } from "@/store/schema-store";

export async function ensureSchemaLoaded(name: string) {
  const store = useSchemaStore.getState();
  let schema = store.getSchema(name);
  if (!schema) {
    const res = await fetch(`${apiDomain}/schemas`);
    const schemas = await res.json();
    store.setSchemasFromArray(schemas);
    schema = store.getSchema(name);
  }
  return schema;
}
