import { defaultBranch, messageListUrl } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import { SchemaReference } from "@/types";
import { buildGithubSchemaUrl } from "./utils";

export async function ensureSchemaLoaded(name: string) {
  const store = useSchemaStore.getState();
  let schema = store.getSchema(name);
  if (!schema) {
    const res = await fetch(messageListUrl);
    const data = (await res.json()) as SchemaReference[];
    const schemas = data.map(({ label, schemaName }) => ({
      label,
      schemaName,
      url: buildGithubSchemaUrl(schemaName, defaultBranch),
    }));
    store.setSchemasFromArray(schemas);
    schema = store.getSchema(name);
  }
  return schema;
}
