const { transports, createLogger, format } = require('winston');

const logger = createLogger({
  level: 'info',
  format: format.combine(
    format.timestamp(),
    format.json(),
    format.errors({ stack: true }), // Ref.: https://stackoverflow.com/a/58475687
    format.colorize(),
    format.prettyPrint()
  ),
  defaultMeta: { service: 'user-service' },
  transports: [
    new transports.Console({ format: format.simple(), timestamp: true }),
    new transports.File({ filename: 'error.log', level: 'error', timestamp: true }),
    new transports.File({ filename: 'combined.log', timestamp: true }),
  ],
});

if (process.env.NODE_ENV === 'production') {
  logger.add(new transports.Console());
}

module.exports = logger;
