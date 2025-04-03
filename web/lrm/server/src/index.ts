import config from './config';
import { logger } from './logger';
import ExpressServer from './expressServer';

class App {
  private expressServer: ExpressServer | undefined;
  public launchServer = async () => {
    try {
      this.expressServer = new ExpressServer(config.URL_PORT);
      this.expressServer.launch();
      logger.info('Express server running');
    } catch (error) {
      console.error(error);
      // @ts-expect-error Will fix this soon
      logger.error(`Express Server failure: ${error.message}`);
      await this.expressServer?.close();
    }
  };
}

new App().launchServer().catch((e) => logger.error(`Error during server launch: ${e}`));
