import { NOMENCLATURE_KEY } from "./config";

export type SchemaReference = {
  label: string;
  schemaName: string;
  url: string;
};

export type RepoReferences = {
  branches: string[];
  tags: string[];
};

export type JsonSchemaProperty = {
  title?: string;
  type?: string;
  description?: string;
  format?: string;
  enum?: (string | number)[];
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
  items?: JsonSchemaProperty;
  minItems?: number;
  maxItems?: number;
  $ref?: string;
  [NOMENCLATURE_KEY]?: string;
};

export type JsonSchemaDefinitions = Record<string, JsonSchemaProperty>;

export type JsonSchemaDocument = JsonSchemaProperty & {
  title?: string;
  definitions?: JsonSchemaDefinitions;
  $defs?: JsonSchemaDefinitions;
};

export type NomenclatureSchema = {
  title?: string;
  description?: string;
  oneOf?: { const: string; title: string; description?: string }[];
};

export type User = {
  username: string;
  avatarUrl: string;
};

export type AuthResponse =
  | { isAuthenticated: true; user: User }
  | { isAuthenticated: false; user: null };

export type ApiUser = {
  username: string;
  avatar_url: string;
};

export type ApiAuthResponse =
  | { authenticated: true; user: ApiUser }
  | { authenticated: false; user: null };
