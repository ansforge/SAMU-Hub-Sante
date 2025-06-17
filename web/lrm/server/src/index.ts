import { logger } from './logger';
import { ExpressServer } from './expressServer';
import { Config } from './config';
import { RabbitMQConnector } from './rabbit/utils';

class App {
  private expressServer: ExpressServer | undefined;
  public launchServer = async () => {
    try {
      const config = new Config();
      this.expressServer = new ExpressServer(config, new RabbitMQConnector(config));
      this.expressServer.launch();
      logger.info('Express server running');
    } catch (error) {
      // @ts-expect-error Will fix this soon
      logger.error(`Express Server failure: ${error.message}`);
      await this.expressServer?.close();
    }
  };
}

new App().launchServer().catch((e) => logger.error(`Error during server launch: ${e}`));
