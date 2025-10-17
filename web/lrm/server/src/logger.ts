import { transports, createLogger, format } from 'winston';

const buildUnexpectedErrorsFileTransport = () =>
  new transports.File({ filename: 'unexpected_errors.log', level: 'error' });

const buildDefaultConsoleTransport = () => new transports.Console({ level: 'info' });

export const logger = createLogger({
  level: 'debug',
  format: format.combine(
    format.timestamp(),
    format.errors({ stack: true }), // Ref.: https://stackoverflow.com/a/5847568
    format.simple(),
  ),
  defaultMeta: { service: 'user-service' },
  transports: [
    buildDefaultConsoleTransport(),
    new transports.File({ filename: 'error.log', level: 'error' }),
    new transports.File({ filename: 'info.log', level: 'info' }),
    new transports.File({ filename: 'combined.log' }),
  ],
  exceptionHandlers: [buildDefaultConsoleTransport(), buildUnexpectedErrorsFileTransport()],
  rejectionHandlers: [buildDefaultConsoleTransport(), buildUnexpectedErrorsFileTransport()],
});
