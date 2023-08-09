# Hub Santé - LRM
_LRM basique afin de pouvoir tester l'envoi / réception de messages_

## Deploy
```bash
cp ../../models/csv_parser/schema.json schemas/
cp ../../models/csv_parser/example.json schemas/
docker buildx build --platform linux/amd64 -t romainfd/hub-lrm:latest .
docker push romainfd/hub-lrm:latest
# Make sure you are on correct Kubernetes context
kubectl replace --force -f ../../hub/infra/web/lrm.yaml
```

## ToDo
### Client
- [ ] Make message sending work
  - [x] Load message from file into UI
  - [ ] Auto add fixed EDXL envelope and message header + integrate header data on send (sender, recipient, time, ...)
  - [ ] Make message editable & send by SAMU A (json)
  - [ ] _Handle XML for SAMU B & NexSIS?_ -> not now
- [ ] Landing interface 
  - [ ] _Choose demo or tests?_ -> not now
  - [ ] Choose userId
    - [ ] tests: can only send to this clientId and will only see messages sent from this clientId to SAMU A, B or NexSIS
    - [ ] demo: SAMU A or SAMU B or NexSIS and can send to these 3
  - [ ] _Security?_ -> not now
    - [ ] ask for the first 10 characters of the associated public cert?
    - [ ] have one password per editor + 1 per SAMUA/SAMUB/NexSIS as env var?
  - [ ] Choose messages
    - [ ] tests: 
      - [ ] use cases list
      - [ ] then stepper with messages to be sent (not mutable)
      - [ ] check received messages to confirm OK
    - [ ] demo: 
      - [ ] messages list to prefill
      - [ ] mutable messages to send
      - [ ] possibility to reuse a received message (to prefill the message sent back as an update or else)
- [ ] Polish UI (design, config, badges, collapsed JSON messages : https://www.npmjs.com/package/vue-json-viewer, ...)

### Server
- [ ] Enable multiple connections
    - [ ] Create a longpoll instance per userId: poll-${userId}/ endpoint on connection and publish to it
      -> for editor tests: userId is the clientId they are using -> check senderId on receive to know where to send
      -> for demo: needs to connect as SAMU A
    - [ ] _Migrate to websockets https://www.npmjs.com/package/ws?_ -> not now (https://chat.openai.com/share/08d3a339-4e8e-40a6-b0e3-7a52bd35292c)
- [ ] Improve logging and visibility to make it easier to debug
    - [ ] Better logging
    - [ ] ELK stack
