const { transports, createLogger, format } = require('winston');

const logger = createLogger({
  level: 'debug',
  format: format.combine(
    format.timestamp(),
    format.json(),
    format.errors({ stack: true }), // Ref.: https://stackoverflow.com/a/58475687
    format.prettyPrint()
  ),
  defaultMeta: { service: 'user-service' },
  transports: [
    new transports.Console({ format: format.simple(), level: 'info', timestamp: true }),
    new transports.File({ filename: 'error.log', level: 'error', timestamp: true }),
    new transports.File({ filename: 'info.log', level: 'info', timestamp: true }),
    new transports.File({ filename: 'combined.log', timestamp: true }),
  ],
});


module.exports = logger;
