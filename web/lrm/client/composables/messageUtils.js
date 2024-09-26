import { DIRECTIONS } from '@/constants'

export function isOut (direction) {
  return direction === DIRECTIONS.OUT
}

const caseIdMap = {
  'RC-EDA': 'createCase.caseId',
  EMSI: 'emsi.EVENT.MAIN_EVENT_ID',
  'RS-EDA': 'createCaseHealth.caseId',
  'RS-EDA-MAJ': 'createCaseHealthUpdate.caseId',
  'RS-RI': 'resourcesInfo.caseId',
  'RS-DR': 'resourcesRequest.caseId',
  'RS-RR': 'resourcesResponse.caseId',
  'RS-SR': 'resourcesStatus.caseId'
}

export function getCaseId (message, isRootMessage = false) {
  if (isRootMessage) {
    message = message.body.content[0].jsonContent.embeddedJsonContent.message
  }
  const caseIdKey = caseIdMap[getMessageKind(message)]
  if (!caseIdKey) {
    return null
  } else {
    return caseIdKey.split('.').reduce((acc, key) => acc[key], message)
  }
}

/**
 * Returns a string representing message type (RC-EDA, EMSI, RS-EDA ou RS-EDA-SMUR)
 * @param {*} message
 */
function getMessageKind (message) {
  if (message.createCase) {
    return 'RC-EDA'
  } else if (message.emsi) {
    return 'EMSI'
  } else if (message.createCaseHealth) {
    return 'RS-EDA'
  } else if (message.createCaseHealthUpdate) {
    return 'RS-EDA-MAJ'
  } else if (message.resourcesInfo) {
    return 'RS-RI'
  } else if (message.resourcesRequest) {
    return 'RS-DR'
  } else if (message.resourcesResponse) {
    return 'RS-RR'
  } else if (message.resourcesStatus) {
    return 'RS-SR'
  }
}

export function getMessageType (message) {
  if (message.body.distributionKind === 'Ack') {
    return 'ack'
  } else if (message.body.distributionKind === 'Error') {
    return 'info'
  } else {
    return 'message'
  }
}
