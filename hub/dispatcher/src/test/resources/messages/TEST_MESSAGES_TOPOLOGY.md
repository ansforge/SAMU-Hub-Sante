# TEST MESSAGES TOPOLOGY

Several test messages are provided to test the dispatcher.

These messages are written in json & xml to test both interfaces. Here is a breif summary of what they're called for.

<u>*Passing cases*:</u>

[samuA_to_nexsis.json](valid/edxl_encapsulated/samuA_to_nexsis.json) - A passing json EDXL to serve as a template for several tests (on TTL, DLQs, etc)

[samuB_to_nexsis.xml](valid/edxl_encapsulated/samuB_to_nexsis.xml) - A passing xml EDXL to serve as a template for several tests (on TTL, DLQs, etc)

[samuA_to_nexsis.xml](valid/edxl_encapsulated/samuA_to_nexsis.xml) - A passing xml EDXL with samuA sender to test sending with samuB routing key, in xml (samuB language)

[samuA_to_samuB.json](valid/edxl_encapsulated/samuA_to_samuB.json) - A passing json EDXL with xml recipient to test xml conversion

[samuB_to_samuA.xml](valid/edxl_encapsulated/samuB_to_samuA.xml) - A passing xml EDXL with json recipient to test json conversion

[createCaseMessage.json](valid/create_case/createCaseMessage.json) and [createCaseMessage.xml](valid/create_case/createCaseMessage.xml) - A passing createCase message (ContentMessage only, for Use case testing purposes).

[genericMessage.json](valid/edxl_encapsulated/genericMessage.json) - A custom message encapsulated in an EDXL-DE wrapper, to test that the Custom Message can be deserialized

<u>*Failing deserialization*:</u>

[edxlWithMalformedContent.json](serialization/edxlWithMalformedContent.json) - A passing EDXL-DE message with malformed content: the createCase.severity value "unknown_enum_value" is not a valid value for this enum.
Deserialization should fail before validation method call.


<u>*Failing validation at EDXL-DE level*:</u>

[missingEDXLRequiredValues](invalid/missingEDXLRequiredValues.json) - A create message encapsulated in an EDXL-DE wrapper with edxl missing fields:
"edxl.distributionID" and "edxl.descriptor.explicitAddress.explicitAddressValue" are missing

<u>*Failing validation at Use case levels*:</u>
- CreateCase: [missingRequiredFieldCreateMessage.json](invalid/create_case/missingRequiredFieldCreateMessage.json) - A create message (use Case only) with "createdAt" missing field



<u>*Edxl validation works when content is not compliant*:</u>

[invalidCreateMessageValidEdxlEnvelope.json](invalid/invalidCreateMessageValidEdxlEnvelope.json) - A create message encapsulated in an EDXL-DE wrapper, with "createdAt" missing field







