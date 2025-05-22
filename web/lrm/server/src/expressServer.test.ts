import { Config } from './config';
import { ExpressServer } from './expressServer';
import { RabbitMQConnector } from './rabbit/utils';

const mockConsume = jest.fn();
const mockConnect = jest.fn((_, callback) => {
  const channel = {
    consume: mockConsume,
    on: jest.fn(),
  };
  const connection = {
    close: jest.fn(),
    on: jest.fn(),
  };
  callback(connection, channel);
});
jest.mock('./rabbit/utils', () => {
  return {
    RabbitMQConnector: jest.fn().mockImplementation(() => {
      return {
        connect: mockConnect,
        close: jest.fn(),
      };
    }),
  };
});

const checkConnectionToVhost = (vhost: string) => {
  expect(mockConnect).toHaveBeenCalledWith(vhost, expect.any(Function));
};

const checkConsumerListenOnQueue = (queue: string) => {
  expect(mockConsume).toHaveBeenCalledWith(queue, expect.any(Function), expect.any(Object));
};

const checkConsumerListenOnClientQueues = (clientId: string) => {
  ['message', 'info', 'ack'].forEach((queueSuffix) => {
    checkConsumerListenOnQueue(`${clientId}.${queueSuffix}`);
  });
};

const originalEnv = process.env;

beforeEach(() => {
  jest.resetModules();
  process.env = {
    ...originalEnv,
    ADMIN_PASSWORD: 'foo',
    HUB_URL: 'foo',
    LRM_PASSPHRASE: 'foo',
    VHOST_CLIENT_MAP: JSON.stringify({
      '15-15_v1.5': ['fr.health.test.samuA', 'fr.health.test.samuB'],
      '15-15_v2.0': ['fr.health.test.samuA', 'fr.health.test.samuB'],
    }),
  };
});

afterEach(async () => {
  jest.restoreAllMocks();
  process.env = originalEnv;
});

describe('Test Connection', () => {
  it('should connect and display logs', async () => {
    const config = new Config();
    const server = new ExpressServer(config, new RabbitMQConnector(config));

    try {
      checkConnectionToVhost('15-15_v1.5');
      checkConsumerListenOnClientQueues('fr.health.test.samuA');
      checkConsumerListenOnClientQueues('fr.health.test.samuB');

      checkConnectionToVhost('15-15_v2.0');
      checkConsumerListenOnClientQueues('fr.health.test.samuA');
      checkConsumerListenOnClientQueues('fr.health.test.samuB');
    } finally {
      // Teardown
      await server.close();
    }
  });
});
