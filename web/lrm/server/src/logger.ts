import { transports, createLogger, format } from 'winston';

const buildDefaultConsoleTransport = () => new transports.Console({ level: 'info' });

export const logger = createLogger({
  level: 'debug',
  format: format.combine(
    format.timestamp(),
    format.errors({ stack: true }),
    format.json(),
  ),
  transports: [buildDefaultConsoleTransport()],
  exceptionHandlers: [buildDefaultConsoleTransport()],
  rejectionHandlers: [buildDefaultConsoleTransport()],
});
