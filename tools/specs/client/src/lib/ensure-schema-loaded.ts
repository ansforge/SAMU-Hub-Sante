import { messageListUrl } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import { SchemaReference } from "@/types";
import { buildGithubSchemaUrl } from "./utils";

export async function ensureSchemaLoaded(name: string, ref: string) {
  const store = useSchemaStore.getState();
  let schema = store.loadedRef === ref ? store.getSchema(name) : undefined;
  if (!schema) {
    const res = await fetch(messageListUrl(ref));
    const data = (await res.json()) as SchemaReference[];
    const schemas = data.map(({ label, schemaName }) => ({
      label,
      schemaName,
      url: buildGithubSchemaUrl(schemaName, ref),
    }));
    store.setSchemasFromArray(schemas, ref);
    schema = store.getSchema(name);
  }
  return schema;
}
