import { transports, createLogger, format } from 'winston';

const buildDefaultConsoleTransport = () => new transports.Console({ level: 'info' });

export const logger = createLogger({
  format: format.combine(
    format.timestamp(),
    format.errors({ stack: true }),
    format.json(),
  ),
  transports: [buildDefaultConsoleTransport()],
  exceptionHandlers: [buildDefaultConsoleTransport()],
  rejectionHandlers: [buildDefaultConsoleTransport()],
});

type MessageLogsMetadata = {
    distributionId?: string
    senderId?: string
    recipientId?: string
}

export const getMessageLogsMetadata = (message: any): MessageLogsMetadata => {
    if (!message) return {};
    const distributionId = message.distributionID;
    const senderId = message.senderID;
    const recipientId = message.descriptor?.explicitAddress?.explicitAddressValue;
    return {
        distributionId,
        senderId,
        recipientId,
    };
}
