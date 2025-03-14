const config = {
  ROOT_DIR: __dirname,
  URL_PORT: process.env.PORT || 8081,
  URL_PATH: process.env.URL || 'http://localhost',
  PROJECT_DIR: __dirname,
  POC_USER_SECRET: process.env.POC_USER_SECRET || 'demo49',
};

module.exports = config;
