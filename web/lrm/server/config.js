const config = {
  ROOT_DIR: __dirname,
  URL_PORT: process.env.PORT || 8081,
  URL_PATH: process.env.URL || 'http://localhost',
  PROJECT_DIR: __dirname,
  ADMIN_PASSWORD: process.env.ADMIN_PASSWORD,
};

module.exports = config;
