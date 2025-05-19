import { ExpressServer } from './expressServer';
import { logger } from './logger';

const mockConsumme = jest.fn();
jest.mock('./rabbit/utils', () => ({
  connect: jest.fn((_, callback) => {
    const channel = {
      consume: mockConsumme,
      on: jest.fn(),
    };
    const connection = {
      close: jest.fn(),
      on: jest.fn(),
    };
    callback(connection, channel);
  }),
  close: jest.fn(() => {}),
  VHOST_CLIENT_MAP: {
    '15-15_v1.5': ['fr.health.samuA', 'fr.health.samuB'],
    '15-15_v2.0': ['fr.health.samuA', 'fr.health.samuB'],
  },
}));

afterEach(async () => {
  jest.restoreAllMocks();
});

describe('Test Connection', () => {
  it('should connect and display logs', async () => {
    const mockLogInfo = jest.spyOn(logger, 'info');

    const server = new ExpressServer(8081);

    try {
      const expectedLogs = [
        'VHOST_CLIENT_MAP: [object Object]',
        ' [*] Waiting for fr.health.samuA messages in fr.health.samuA.message (15-15_v1.5). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuA messages in fr.health.samuA.info (15-15_v1.5). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuA messages in fr.health.samuA.ack (15-15_v1.5). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuB messages in fr.health.samuB.message (15-15_v1.5). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuB messages in fr.health.samuB.info (15-15_v1.5). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuB messages in fr.health.samuB.ack (15-15_v1.5). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuA messages in fr.health.samuA.message (15-15_v2.0). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuA messages in fr.health.samuA.info (15-15_v2.0). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuA messages in fr.health.samuA.ack (15-15_v2.0). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuB messages in fr.health.samuB.message (15-15_v2.0). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuB messages in fr.health.samuB.info (15-15_v2.0). To exit press CTRL+C',
        ' [*] Waiting for fr.health.samuB messages in fr.health.samuB.ack (15-15_v2.0). To exit press CTRL+C',
      ];
      expectedLogs.forEach((message) => {
        expect(mockLogInfo).toHaveBeenCalledWith(message);
      });

      const expectedLogInfoCount = expectedLogs.length;
      expect(mockLogInfo).toHaveBeenCalledTimes(expectedLogInfoCount);
    } catch (err) {
      throw err;
    } finally {
      // Teardown
      await server.close();
    }
  });
});
