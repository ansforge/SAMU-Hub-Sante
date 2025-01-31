import moment from 'moment/moment';
import { v4 as uuidv4 } from 'uuid';
import { consola } from 'consola';
import { clientInfos } from './userUtils';
import { EDXL_ENVELOPE, DIRECTIONS } from '@/constants';
import { useMainStore } from '~/store';
import { useAuthStore } from '~/store/auth';

const VALUES_TO_DROP = [null, undefined, ''];

export function isOut(direction) {
  return direction === DIRECTIONS.OUT;
}

export const ValidationStatus = {
  VALID: 'valid',
  APPROXIMATE: 'approximate',
  INVALID: 'invalid',
};

const caseIdMap = {
  'RC-EDA': 'createCase.caseId',
  EMSI: 'emsi.EVENT.MAIN_EVENT_ID',
  'RS-EDA': 'createCaseHealth.caseId',
  'RS-EDA-MAJ': 'createCaseHealthUpdate.caseId',
  'RS-RI': 'resourcesInfo.caseId',
  'RS-DR': 'resourcesRequest.caseId',
  'RS-RR': 'resourcesResponse.caseId',
  'RS-SR': 'resourcesStatus.caseId',
};

export function getCaseId(message, isRootMessage = false) {
  if (isRootMessage) {
    message = message.body.content[0].jsonContent.embeddedJsonContent.message;
  }
  const caseIdKey = caseIdMap[getMessageKind(message)];
  if (!caseIdKey) {
    return null;
  } else {
    return caseIdKey.split('.').reduce((acc, key) => acc[key], message);
  }
}

export function getDistributionIdOfAckedMessage(message) {
  return message?.body?.content?.[0]?.jsonContent?.embeddedJsonContent?.message
    ?.reference?.distributionID;
}

/**
 * Returns a string representing message type (RC-EDA, EMSI, RS-EDA ou RS-EDA-SMUR)
 * @param {*} message
 */
function getMessageKind(message) {
  if (message.createCase) {
    return 'RC-EDA';
  } else if (message.emsi) {
    return 'EMSI';
  } else if (message.createCaseHealth) {
    return 'RS-EDA';
  } else if (message.createCaseHealthUpdate) {
    return 'RS-EDA-MAJ';
  } else if (message.resourcesInfo) {
    return 'RS-RI';
  } else if (message.resourcesRequest) {
    return 'RS-DR';
  } else if (message.resourcesResponse) {
    return 'RS-RR';
  } else if (message.resourcesStatus) {
    return 'RS-SR';
  } else if (message.rpis) {
    return 'RPIS';
  }
}

export function getMessageType(message) {
  if (message.body.distributionKind === 'Ack') {
    return 'ack';
  } else if (message.body.distributionKind === 'Error') {
    return 'info';
  } else {
    return 'message';
  }
}

/**
 * Sets the case ID in the message to the current case ID
 * @param {*} message
 * @param {*} caseId
 * @param {*} localCaseId
 */
export function setCaseId(message, caseId, localCaseId) {
  switch (getMessageKind(message)) {
    case 'RC-EDA':
      message.createCase.caseId = caseId;
      message.createCase.senderCaseId = localCaseId;
      break;
    case 'EMSI':
      message.emsi.EVENT.MAIN_EVENT_ID = caseId;
      message.emsi.EVENT.ID = localCaseId;
      break;
    case 'RS-EDA':
      message.createCaseHealth.caseId = caseId;
      message.createCaseHealth.senderCaseId = localCaseId;
      break;
    case 'RS-EDA-MAJ':
      message.createCaseHealthUpdate.caseId = caseId;
      message.createCaseHealthUpdate.senderCaseId = localCaseId;
      break;
    case 'RS-RI':
      message.resourcesInfo.caseId = caseId;
      break;
    case 'RS-SR':
      message.resourcesStatus.caseId = caseId;
      break;
    case 'RS-DR':
      message.resourcesRequest.caseId = caseId;
      break;
    case 'RS-RR':
      message.resourcesResponse.caseId = caseId;
      break;
    case 'RPIS':
      message.rpis.context.caseId = caseId;
      break;
  }
}

export function buildAck(distributionID) {
  return buildMessage({ reference: { distributionID } }, 'Ack');
}

export function buildMessage(innerMessage, distributionKind = 'Report') {
  // InnerMessage should only have one key, as we do not support multiple use cases in the same message.
  const useCase = Object.keys(innerMessage)[0];
  if (Object.keys(innerMessage).length > 1) {
    throw new Error('Inner message should only have one key');
  }
  const store = useMainStore();
  const authStore = useAuthStore();
  const message = JSON.parse(JSON.stringify(EDXL_ENVELOPE)); // Deep copy
  if (/^((1\.)|(2\.))/.test(store.selectedVhost.modelVersion)) {
    // We delete 'keyword' from 'descriptor' if the model version is 1.* or 2.*
    delete message.descriptor.keyword;
  } else {
    message.descriptor.keyword[0].value = useCase;
  }
  const formattedInnerMessage = formatIdsInMessage(innerMessage);
  message.content[0].jsonContent.embeddedJsonContent.message = {
    ...message.content[0].jsonContent.embeddedJsonContent.message,
    ...formattedInnerMessage,
  };
  const name = clientInfos().name;
  const targetId = authStore.user.targetId;
  const sentAt = moment().format();
  message.distributionID = `${authStore.user.clientId}_${uuidv4()}`;
  message.distributionKind = distributionKind;
  message.senderID = authStore.user.clientId;
  message.dateTimeSent = sentAt;
  message.descriptor.explicitAddress.explicitAddressValue = targetId;
  message.content[0].jsonContent.embeddedJsonContent.message.messageId =
    message.distributionID;
  message.content[0].jsonContent.embeddedJsonContent.message.kind =
    message.distributionKind;
  message.content[0].jsonContent.embeddedJsonContent.message.sender = {
    name,
    URI: `hubex:${authStore.user.clientId}`,
  };
  message.content[0].jsonContent.embeddedJsonContent.message.sentAt = sentAt;
  message.content[0].jsonContent.embeddedJsonContent.message.recipient = [
    {
      name: clientInfos(authStore.user.targetId).name,
      URI: `hubex:${targetId}`,
    },
  ];
  return trimEmptyValues(message);
}

function formatIdsInMessage(innerMessage) {
  // Check the entire message for occurences of {senderName} and replace it with the actual sender name
  const senderName = clientInfos().name;
  let jsonString = JSON.stringify(innerMessage);
  jsonString = jsonString.replaceAll('{senderName}', senderName);
  return JSON.parse(jsonString);
}

export function trimEmptyValues(obj) {
  return Object.entries(obj).reduce((acc, [key, value]) => {
    if (!(VALUES_TO_DROP.includes(value) || isEmpty(value))) {
      if (typeof value !== 'object') {
        acc[key] = value;
      } else {
        value = trimEmptyValues(value);
        if (!isEmpty(value)) {
          acc[key] = value;
        }
      }
    }
    return Array.isArray(obj) ? Object.values(acc) : acc;
  }, {});
}

function isEmpty(obj) {
  if (typeof obj === 'object') {
    return Object.keys(obj).length === 0;
  }
  return false;
}

export function sendMessage(msg, vhost = null) {
  const store = useMainStore();
  const authStore = useAuthStore();
  if (store.socket?.readyState === 1) {
    if (!vhost) {
      vhost = store.selectedVhost.vhost;
    }
    try {
      consola.log('Sending message', msg);
      store.socket.send(
        JSON.stringify({ key: authStore.user.clientId, vhost, msg })
      );
      store.addMessage({
        direction: DIRECTIONS.OUT,
        vhost,
        routingKey: authStore.user.targetId,
        time: timeDisplayFormat(),
        messageType: getReadableMessageType(msg.distributionKind),
        body: msg,
      });
    } catch (e) {
      alert(`Erreur lors de l'envoi du message: ${e}`);
    }
  } else {
    // TODO: Add proper retry logic here with either exponential backoff or a retry limit
    consola.log('Socket is not open. Retrying in half a second.');
    setTimeout(() => {
      sendMessage(msg);
    }, 500);
  }
}

export function timeDisplayFormat() {
  const d = new Date();
  return (
    d.toLocaleTimeString('fr').replace(':', 'h') +
    '.' +
    String(new Date().getMilliseconds()).padStart(3, '0')
  );
}

export function getReadableMessageType(messageType) {
  switch (messageType) {
    case 'Ack':
      return 'Ack';
    case 'Error':
      return 'Info';
    default:
      return 'Message';
  }
}
