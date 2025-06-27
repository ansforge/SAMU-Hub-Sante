import { RabbitMQConnector } from './rabbit/utils';
import { WebSocket } from 'ws';
import { WebSocketHandler } from './WebSocketHandler';
import { Config } from './config';
import { EventEmitter } from 'stream';

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

// Mock RabbitMQConnector
const mockPublish = vi.fn();
const mockConnectAsync = vi.fn(async (vhost: string) => {
  const channel = {
    publish: mockPublish,
    on: vi.fn(),
  };
  const connection = {
    close: vi.fn(),
    on: vi.fn(),
  };
  return Promise.resolve({ connection, channel });
});
vi.mock('./rabbit/utils', () => {
  return {
    RabbitMQConnector: vi.fn().mockImplementation(() => {
      return {
        connectAsync: mockConnectAsync,
        close: vi.fn(),
      };
    }),
  };
});

vi.mock('ws', () => {
  return {
    WebSocket: vi.fn().mockImplementation(() => {
      return new EventEmitter();
    }),
  };
});

describe('WebSocketHandler sendMessage', () => {
  it('sends a message when receiving web socket "message" event', async () => {
    const config = new Config();
    const mockSocket = new WebSocket(null);
    const wsHandler = new WebSocketHandler(mockSocket, config, new RabbitMQConnector(config));
    wsHandler.listen();
    const socketMessage = { vhost: '15-15_v1.5', key: 'foo', msg: { distributionID: 'fr.health.test.samuA_123456' } };

    mockSocket.emit('message', JSON.stringify(socketMessage));

    expect(mockConnectAsync).toHaveBeenCalledWith('15-15_v1.5');
    // Wait for the async call to mockConnectAsync to resolve
    // before expecting mockPublish to have been called
    await mockConnectAsync.mock.results[0].value;
    expect(mockPublish).toHaveBeenCalledWith('hubsante', 'foo', Buffer.from(JSON.stringify(socketMessage.msg)), {
      contentType: 'application/json',
      deliveryMode: 2,
      priority: 0,
    });
  });
});
