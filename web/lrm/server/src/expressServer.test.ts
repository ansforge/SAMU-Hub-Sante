import { ExpressServer } from './expressServer';
import { logger } from './logger';

const mockConsumme = jest.fn();
jest.mock('./rabbit/utils', () => ({
  connect: jest.fn((_, callback) => {
    const channel = {
      consume: mockConsumme,
    };
    const connection = {
      close: jest.fn(),
    };
    callback(connection, channel);
  }),
  close: jest.fn(() => {}),
  VHOSTS: {
    '15-15_v1.5': '1.0.0',
    '15-15_v2.0': '2.0.0',
  },
  DEMO_CLIENT_IDS: [
    ['fr.health.samuA', ['fr.health.samuB']],
    ['fr.health.samuB', ['fr.health.samuA']],
  ],
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
        'Demo client ids: fr.health.samuA,fr.health.samuB,fr.health.samuB,fr.health.samuA',
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
