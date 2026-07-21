export type SchemaReference = {
  name: string;
  path: string;
  sha: string;
  url: string;
};

export type JsonSchemaProperty = {
  type?: string;
  description?: string;
};

export type JsonSchemaDocument = {
  title?: string;
  description?: string;
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
};