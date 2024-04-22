# Hub Santé - LRM
_LRM basique afin de pouvoir tester l'envoi / réception de messages_

## Deploy
```bash
# Build UI
export BACKEND_LRM_SERVER=hub.esante.gouv.fr # or another domain depending on environment (must be explicit in the image tag), as we must pass it at nuxt build time
cd server && npm run setup && cd ..
# Build & push docker image
docker buildx build --platform linux/amd64 -t romainfd/hub-lrm:latest .
docker push romainfd/hub-lrm:latest
cd client && npm build
docker buildx build --platform linux/amd64 -t romainfd/hub-lrm-client:latest .


# Redo it for the preprod environment (quick and dirty way to ensure preprod and prod are built on the same codebase, even if we have to pass
# env variable at build time - we should handle it differently later)
# Build UI
export BACKEND_LRM_SERVER=pre-prod.hub.esante.gouv.fr # or another domain depending on environment (must be explicit in the image tag), as we must pass it at nuxt build time
cd server && npm run setup && cd ..
# Build & push docker image
docker buildx build --platform linux/amd64 -t romainfd/hub-lrm:preprod .
docker push romainfd/hub-lrm:preprod
cd client && npm build
docker buildx build --platform linux/amd64 -t romainfd/hub-lrm-client:preprod .

# Make sure you are on correct Kubernetes context
kubectl replace --force -f ../../hub/infra/web/lrm.yaml
kubectl replace --force -f ../../hub/infra/web/lrm-client.yaml
```

## Local development
### Client
```bash
# Using local server
BACKEND_LRM_SERVER=localhost npm run dev

# Using prod remote server
BACKEND_LRM_SERVER=hub.esante.gouv.fr npm run dev
```

### Server
```bash
# Using local RabbitMQ (within Kubernetes)
HUB_URL=amqps://rabbitmq.default.svc npm run dev

# Using prod remote RabbitMQ
HUB_URL=amqps://messaging.hub.esante.gouv.fr npm run dev
```

## ToDo
### Client
- [x] Make message sending work
  - [x] Load message from file into UI
  - [x] Auto add fixed EDXL envelope and message header + integrate header data on send (sender, recipient, time, ...)
  - [x] Rebuild local cluster
  - [x] Review DLQ PR and merge into demo branch
  - [x] Review Model Migration PR and merge into demo branch
  - [x] Use local dispatcher image for tests
  - [x] Make message editable & send by SAMU A (json)
  - [ ] _Handle XML for SAMU B & NexSIS?_ -> not now
- [ ] Landing interface
  - [x] _Choose demo or tests?_ -> not now
  - [ ] Choose userId, targetId and tester or not
    - [x] Can only see messages between these entities from userId point of view (A -> B out or B -> A in)
    - [x] demo: list of message types with prebuild messages appearing on top to send messages & edit them if wanted 
    - [ ] tests: list of use cases with steps and go through (send or validate recep ok) to validate the use case - no edit
  - [ ] _Security?_ -> not now
    - [ ] ask for the first 10 characters of the associated public cert?
    - [ ] have one password per editor + 1 per SAMUA/SAMUB/NexSIS as env var?
  - [x] Advanced mode
    - [x] Ability to live switch between users
  - [ ] Choose messages
    - [ ] tests: 
      - [ ] use cases list
      - [ ] then stepper with messages to be sent (not mutable)
      - [ ] check received messages to confirm OK and move to the next step
    - [x] demo: 
      - [x] messages types with message list to prefill
      - [x] mutable messages to send
      - [x] possibility to reuse a received message (to prefill the message sent back as an update or else)
- [x] Polish UI (design, config, badges, collapsed JSON messages : https://www.npmjs.com/package/vue-json-viewer, ...)

### Server
- [x] Enable multiple connections
    - [x] Create a longpoll instance per pair: poll-${userId}-${targetId}/ endpoint on connection and publish to it
      -> for editor tests: userId is the clientId they are using -> check senderId on receive to know where to send
      -> for demo: needs to connect as SAMU A
    - [x] _Migrate to websockets https://www.npmjs.com/package/ws?_ -> not now (https://chat.openai.com/share/08d3a339-4e8e-40a6-b0e3-7a52bd35292c)
- [ ] Improve logging and visibility to make it easier to debug
    - [ ] Better logging
    - [ ] ELK stack

### Acks
- [x] Be able to send back acks manually or automatically
- [x] Handle ack display in UI

### Improvements
- [x] Group by caseId (conversation mode)
- [ ] Add documentation
