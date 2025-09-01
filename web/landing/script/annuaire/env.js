import { Environment } from "./constants.js";

let selectedEnv = Environment.PROD;

export function setSelectedEnv(env) {
  selectedEnv = env;
}

export function getSelectedEnv() {
  return selectedEnv;
}
