const config = require('./config');
const logger = require('./logger');
const ExpressServer = require('./expressServer');

const launchServer = async () => {
  try {
    this.expressServer = new ExpressServer(config.URL_PORT);
    this.expressServer.launch();
    logger.info('Express server running');
  } catch (error) {
    console.error(error);
    logger.error(`Express Server failure: ${error.message}`);
    await this.expressServer.close();
  }
};

launchServer().catch((e) => logger.error(`Error during server launch: ${e}`));
