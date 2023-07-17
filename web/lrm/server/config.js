const path = require('path');

const config = {
  ROOT_DIR: __dirname,
  URL_PORT: process.env.PORT || 8081,
  URL_PATH: process.env.URL || 'http://localhost',
  PROJECT_DIR: __dirname,
};
config.ASYNCAPI_YAML = path.join(config.ROOT_DIR, 'hubsante.asyncapi.yaml');

module.exports = config;
