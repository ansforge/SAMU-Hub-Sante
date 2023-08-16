export const DIRECTIONS = {
  IN: '←',
  OUT: '→'
}
export const WRONG_EDXL_ENVELOPE = {
  distributionID: '{{ samuA_2608323d-507d-4cbf-bf74-52007f8124ea }}',
  senderID: '{{ fr.health.samuA }}',
  dateTimeSent: '{{ 2022-09-27T08:23:34+02:00 }}',
  dateTimeExpires: '2072-09-27T08:23:34+02:00',
  distributionStatus: 'Actual',
  distributionKind: 'Report',
  descriptor: {
    language: 'fr-FR',
    explicitAddress: {
      explicitAddressScheme: 'hubsante',
      explicitAddressValue: '{{ fr.health.samuB }}'
    }
  },
  content: {
    contentObject: {
      jsonContent: {
        embeddedJsonContent: {
          message: {}
        }
      }
    }
  }
}
export const EDXL_ENVELOPE = {
  distributionID: '{{ samuA_2608323d-507d-4cbf-bf74-52007f8124ea }}',
  senderID: '{{ fr.health.samuA }}',
  dateTimeSent: '{{ 2022-09-27T08:23:34+02:00 }}',
  dateTimeExpires: '2072-09-27T08:23:34+02:00',
  distributionStatus: 'Actual',
  distributionKind: '{{ Report }}',
  descriptor: {
    language: 'fr-FR',
    explicitAddress: {
      explicitAddressScheme: 'hubsante',
      explicitAddressValue: '{{ fr.health.samuB }}'
    }
  },
  content: {
    contentObject: {
      jsonContent: {
        embeddedJsonContent: {
          message: {
            messageId: '{{ 2608323d-507d-4cbf-bf74-52007f8124ea }}',
            sender: {
              name: '{{ samuA }}',
              uri: '{{ hubsante:fr.health.samuA }}'
            },
            sentAt: '{{ 2022-09-27T08:23:34+02:00 }}',
            msgType: 'ALERT',
            status: 'TEST',
            recipients: {
              recipient: [
                {
                  name: '{{ samuB }}',
                  uri: '{{ hubsante:fr.health.samuB }}'
                }
              ]
            }
          }
        }
      }
    }
  }
}
