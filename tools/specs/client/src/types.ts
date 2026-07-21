export type SchemaReference = {
  name: string;
  path: string;
  sha: string;
  url: string;
};

export type JsonSchemaProperty = {
  type?: string;
  description?: string;
  format?: string;
  enum?: (string | number)[];
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
  items?: JsonSchemaProperty;
  $ref?: string;
};

export type JsonSchemaDefinitions = Record<string, JsonSchemaProperty>;

export type JsonSchemaDocument = JsonSchemaProperty & {
  title?: string;
  definitions?: JsonSchemaDefinitions;
  $defs?: JsonSchemaDefinitions;
};
