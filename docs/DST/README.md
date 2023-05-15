# Dossier des Spécifications Techniques (DST)
Ce répertoire regroupe le DST et les ressources associées :
- [`EDXL.xsd`](../../hub/dispatcher/src/main/resources/xsd/edxl/edxl-de-v2.0-wd11.xsd) est le XSD spécifiant l'enveloppe EDXL-DE en XML
- [`EDXL-CisuCreateExemple.asyncapi.yaml`](../../models/hubsante.asyncapi.yaml) est l'AsyncAPI spécifiant l'enveloppe EDXL-DE en JSON. A titre d'exemple, l'enveloppe contient les spécifications d'un message basé sur un CreateEvent du modèle CISU v1.12 (mai 2022).
- `EDXL-CisuCreateExemple.{json,xml}` sont des exemples (respectivement en [JSON](../../hub/dispatcher/src/test/resources/cisuCreateEdxl.json) et [XML](../../hub/dispatcher/src/test/resources/cisuCreateEdxl.xml)) d'une enveloppe EDXL-DE contenant un message basé sur un CreateEvent du modèle CISU v1.12 (mai 2022).
