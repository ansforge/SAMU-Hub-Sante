import { Config } from '../config';
import { RabbitMQConnector } from '../rabbit/utils';
import { EventEmitter } from 'stream';
import { MessagingService } from './messaging';
import { WebSocketServer } from 'ws';

const mockConsume = vi.fn();
const mockChannel = new EventEmitter();
// @ts-expect-error
mockChannel.consume = mockConsume;
const mockConnection = {
  createChannel: vi.fn((callback) => {
    callback(null, mockChannel);
  }),
  close: vi.fn(),
  on: vi.fn(),
};

const mockConnect = vi.fn((_: any, callback: any) => {
  callback(mockConnection, mockChannel);
});

// Mock RabbitMQConnector
vi.mock('../rabbit/utils', () => {
  return {
    RabbitMQConnector: vi.fn().mockImplementation(() => {
      return {
        connect: mockConnect,
        close: vi.fn(),
      };
    }),
  };
});

const originalEnv = process.env;

const reconnectSpy = vi.spyOn(MessagingService.prototype, 'reconnect');
const handleChannelErrorSpy = vi.spyOn(MessagingService.prototype, 'handleChannelError');

beforeEach(() => {
  vi.resetModules();
  process.env = {
    ...originalEnv,
    ADMIN_PASSWORD: 'foo',
    HUB_URL: 'foo',
    LRM_PASSPHRASE: 'foo',
    VHOST_CLIENT_MAP: JSON.stringify({
      '15-15_v1.5': ['fr.health.test.samuV1'],
    }),
  };
});

afterEach(async () => {
  vi.clearAllMocks();
  process.env = originalEnv;
});

describe('Messaging service', () => {
  it('crash the app after trying to reconnect 3 times', async () => {
    const config = new Config();
    const connector = new RabbitMQConnector(config);

    const service = new MessagingService('15-15_v1.5', config, connector, new WebSocketServer({ noServer: true }));

    service.connectToVhost();
    for (let i = 0; i < 3; i++) {
      mockChannel.emit('error', new Error('Mock error during connection to queue'));
    }

    expect(handleChannelErrorSpy).toHaveBeenCalledTimes(3);
    expect(reconnectSpy).toHaveBeenCalledTimes(3);

    try {
      mockChannel.emit('error', new Error('Mock error during connection to queue'));
    } catch (err) {
      expect(err).toEqual(new Error('Mock error during connection to queue'));
    }

    // Check that all the expect statements have been checked (wether valid or failing)
    expect.assertions(3);
  });

  it('crash the app after receiving a missing queue error', async () => {
    const config = new Config();
    const connector = new RabbitMQConnector(config);

    const service = new MessagingService('15-15_v1.5', config, connector, new WebSocketServer({ noServer: true }));

    service.connectToVhost();

    try {
      mockChannel.emit('error', {
        code: 404,
        message: 'NOT_FOUND - no queue fr.health.test.samuV1',
      });
    } catch (err) {
      expect(handleChannelErrorSpy).toHaveBeenCalledTimes(1);
      expect(reconnectSpy).not.toHaveBeenCalled();
      expect(err).toEqual({
        code: 404,
        message: 'NOT_FOUND - no queue fr.health.test.samuV1',
      });
    }
    // Check that all the expect statements have been checked (wether valid or failing)
    expect.assertions(3);
  });
});
