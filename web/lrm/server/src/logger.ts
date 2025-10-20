import { transports, createLogger, format } from 'winston';

const buildDefaultConsoleTransport = () => new transports.Console({ level: 'info' });

export const logger = createLogger({
  level: 'debug',
  format: format.combine(
    format.timestamp(),
    format.errors({ stack: true }), // Ref.: https://stackoverflow.com/a/5847568
    format.simple(),
  ),
  defaultMeta: { service: 'user-service' },
  transports: [buildDefaultConsoleTransport()],
  exceptionHandlers: [buildDefaultConsoleTransport()],
  rejectionHandlers: [buildDefaultConsoleTransport()],
});
