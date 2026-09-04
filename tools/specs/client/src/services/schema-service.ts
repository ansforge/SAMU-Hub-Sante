import { apiDomain } from "@/config";

export type SchemaUpdateResponse = {
  commit_url: string;
  schema_id: string;
  status: string;
};

export type UpdateSchemaVariables = {
  body: {
    data: string;
    ref: string;
    commit_message: string;
    new_branch: string | null;
  };
  schemaId: string;
};

export const SchemaService = {
  update: async ({
    body,
    schemaId,
  }: UpdateSchemaVariables): Promise<SchemaUpdateResponse> => {
    const res = await fetch(`${apiDomain}/repo/schema/${schemaId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      credentials: "include",
    });
    if (!res.ok) throw new Error("Erreur lors de la modification du schema.");
    return res.json();
  },
};
