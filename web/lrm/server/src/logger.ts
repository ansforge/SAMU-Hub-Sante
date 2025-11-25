import { transports, createLogger, format } from 'winston';

const logLevel = process.env.LOG_LEVEL || 'info';

const buildDefaultConsoleTransport = () => new transports.Console({ level: logLevel });

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
    messageType?: string
}

const getMessageType = (message: any): string | undefined => {
    const messageContent = message?.content?.[0]?.jsonContent?.embeddedJsonContent?.message ?? {};
    const excludedKeys = ["messageId", "sender", "sentAt", "kind", "status", "recipient"];
    const candidateKeys = Object.keys(messageContent)
        .filter(k => !excludedKeys.includes(k));

    // Return the message type if there's exactly one candidate key, otherwise we can't determine it
    return candidateKeys.length === 1 ? candidateKeys[0] : undefined;

}

export const getMessageLogsMetadata = (message: any): MessageLogsMetadata => {
    if (!message) return {};
    const distributionId = message.distributionID;
    const senderId = message.senderID;
    const recipientId = message.descriptor?.explicitAddress?.explicitAddressValue;
    const messageType = getMessageType(message);
    return {
        distributionId,
        senderId,
        recipientId,
        messageType,
    };
}
