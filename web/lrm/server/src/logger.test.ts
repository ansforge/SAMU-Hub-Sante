import { describe, it, expect } from 'vitest';
import { getMessageLogsMetadata } from './logger';

describe('getMessageLogsMetadata', () => {
    it('should return metadata when existing', () => {
        const message = {
            distributionID: 'dist-123',
            senderID: 'sender-456',
            descriptor: {
                explicitAddress: {
                    explicitAddressValue: 'recipient-789',
                },
            },
            content: [
                {
                    jsonContent: {
                        embeddedJsonContent: {
                            message: {
                                createCaseHealth: {},
                                messageId: 'msg-001',
                                sender: 'sender-456',
                            }
                        },
                    },
                },
            ],
        };
        const meta = getMessageLogsMetadata(message);
        expect(meta).toEqual({
            distributionId: 'dist-123',
            senderId: 'sender-456',
            recipientId: 'recipient-789',
            messageType: 'createCaseHealth',
        });
    });

    it('should return recipientId undefined if descriptor is missing', () => {
        const message = {
            distributionID: 'dist-1',
            senderID: 'sender-2',
            content: [
                {
                    jsonContent: {
                        embeddedJsonContent: {
                            message: {
                                createCaseHealth: {},
                                messageId: 'msg-001',
                                sender: 'sender-456',
                            }
                        },
                    },
                },
            ],
        };
        const meta = getMessageLogsMetadata(message);
        expect(meta).toEqual({
            distributionId: 'dist-1',
            senderId: 'sender-2',
            recipientId: undefined,
            messageType: 'createCaseHealth',
        });
    });

    it('should return recipientId undefined if explicitAddress is missing', () => {
        const message = {
            distributionID: 'dist-X',
            senderID: 'sender-Y',
            descriptor: {},
            content: [
                {
                    jsonContent: {
                        embeddedJsonContent: {
                            message: {
                                createCaseHealth: {},
                                messageId: 'msg-001',
                                sender: 'sender-456',
                            }
                        },
                    },
                },
            ],
        };
        const meta = getMessageLogsMetadata(message);
        expect(meta).toEqual({
            distributionId: 'dist-X',
            senderId: 'sender-Y',
            recipientId: undefined,
            messageType: 'createCaseHealth',
        });
    });

    it('should return recipientId undefined if explicitAddressValue missing', () => {
        const message = {
            distributionID: 'dist-A',
            senderID: 'sender-B',
            descriptor: {
                explicitAddress: {},
            },
            content: [
                {
                    jsonContent: {
                        embeddedJsonContent: {
                            message: {
                                createCaseHealth: {},
                                messageId: 'msg-001',
                                sender: 'sender-456',
                            }
                        },
                    },
                },
            ],
        };
        const meta = getMessageLogsMetadata(message);
        expect(meta).toEqual({
            distributionId: 'dist-A',
            senderId: 'sender-B',
            recipientId: undefined,
            messageType: 'createCaseHealth',
        });
    });

    it('should return undefined for each missing field', () => {
        const message = {};
        const meta = getMessageLogsMetadata(message);
        expect(meta).toEqual({
            distributionId: undefined,
            senderId: undefined,
            recipientId: undefined,
            messageType: undefined,
        });
    });

    it('should handle null values', () => {
        const message = {
            distributionID: null,
            senderID: null,
            descriptor: {
                explicitAddress: {
                    explicitAddressValue: null,
                },
            },
            content: null,
        };
        const meta = getMessageLogsMetadata(message);
        expect(meta).toEqual({
            distributionId: null,
            senderId: null,
            recipientId: null,
            messageType: undefined,
        });
    });

    it('should return empty object for undefined message', () => {
        const meta = getMessageLogsMetadata(undefined);
        expect(meta).toEqual({});
    });

    it('should return undefined messageType when multiple candidate keys', () => {
        const message = {
            distributionID: 'dist-123',
            senderID: 'sender-456',
            descriptor: {
                explicitAddress: {
                    explicitAddressValue: 'recipient-789',
                },
            },
            content: [
                {
                    jsonContent: {
                        embeddedJsonContent: {
                            message: {
                                createCaseHealth: {},
                                createCaseHealthUpdate: {},
                                messageId: 'msg-001',
                                sender: 'sender-456',
                            }
                        }
                    }
                }
            ]
        }

        const metadata = getMessageLogsMetadata(message);
        expect(metadata).toEqual({
            distributionId: 'dist-123',
            senderId: 'sender-456',
            recipientId: 'recipient-789',
            messageType: undefined,
        });
    })

    it('should return undefined messageType when no candidate keys', () => {
        const message = {
            distributionID: 'dist-123',
            senderID: 'sender-456',
            descriptor: {
                explicitAddress: {
                    explicitAddressValue: 'recipient-789',
                },
            },
            content: [
                {
                    jsonContent: {
                        embeddedJsonContent: {
                            message: {
                                messageId: 'msg-001',
                                sender: 'sender-456',
                            }
                        }
                    }
                }
            ]
        }

        const metadata = getMessageLogsMetadata(message);
        expect(metadata).toEqual({
            distributionId: 'dist-123',
            senderId: 'sender-456',
            recipientId: 'recipient-789',
            messageType: undefined,
        });
    })
});
