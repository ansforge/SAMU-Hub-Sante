import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Config } from './config';
import { ExpressServer } from './expressServer';
import { RabbitMQConnector } from './rabbit/utils';

const mockConsume = vi.fn();
const mockConnect = vi.fn((_: any, callback: any) => {
  const channel = {
    consume: mockConsume,
    on: vi.fn(),
  };
  const connection = {
    close: vi.fn(),
    on: vi.fn(),
  };
  callback(connection, channel);
});

// Mock RabbitMQConnector
vi.mock('./rabbit/utils', () => {
  return {
    RabbitMQConnector: vi.fn().mockImplementation(() => {
      return {
        connect: mockConnect,
        close: vi.fn(),
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
  vi.resetModules();
  process.env = {
    ...originalEnv,
    ADMIN_PASSWORD: 'foo',
    HUB_URL: 'foo',
    LRM_PASSPHRASE: 'foo',
    VHOST_CLIENT_MAP: JSON.stringify({
      '15-15_v1.5': ['fr.health.test.samuV1', 'fr.health.test.samuV2'],
      '15-15_v2.0': ['fr.health.test.samuV1', 'fr.health.test.samuV2'],
    }),
  };
});

afterEach(async () => {
  vi.restoreAllMocks();
  process.env = originalEnv;
});

describe('Test Connection', () => {
  it('should connect and display logs', async () => {
    const config = new Config();
    const server = new ExpressServer(config, new RabbitMQConnector(config));

    try {
      checkConnectionToVhost('15-15_v1.5');
      checkConsumerListenOnClientQueues('fr.health.test.samuV1');
      checkConsumerListenOnClientQueues('fr.health.test.samuV2');

      checkConnectionToVhost('15-15_v2.0');
      checkConsumerListenOnClientQueues('fr.health.test.samuV1');
      checkConsumerListenOnClientQueues('fr.health.test.samuV2');
    } finally {
      // Teardown
      await server.close();
    }
  });
});
