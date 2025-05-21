import { ExpressServer } from './expressServer';
import * as utils from './rabbit/utils';

const mockConsume = jest.fn();
jest.mock('./rabbit/utils', () => ({
  connect: jest.fn((_, callback) => {
    const channel = {
      consume: mockConsume,
      on: jest.fn(),
    };
    const connection = {
      close: jest.fn(),
      on: jest.fn(),
    };
    callback(connection, channel);
  }),
  close: jest.fn(() => {}),
}));
const mockedUtils = utils as jest.Mocked<typeof utils>;

const checkConnectionToVhost = (vhost: string) => {
  expect(mockedUtils.connect).toHaveBeenCalledWith(vhost, expect.any(Function));
};

const checkConsumerListenOnQueue = (queue: string) => {
  expect(mockConsume).toHaveBeenCalledWith(queue, expect.any(Function), expect.any(Object));
};

const checkConsumerListenOnClientQueues = (clientId: string) => {
  ['message', 'info', 'ack'].forEach((queueSuffix) => {
    checkConsumerListenOnQueue(`${clientId}.${queueSuffix}`);
  });
};

afterEach(async () => {
  jest.restoreAllMocks();
});

describe('Test Connection', () => {
  it('should connect and display logs', async () => {
    const server = new ExpressServer(8081);

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
