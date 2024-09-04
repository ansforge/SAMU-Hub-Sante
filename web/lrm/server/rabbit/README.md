# Local run
The certificate in `certs/` is the one of the integration LRM.
The `certs/rootCA.crt` is IGC TEST.

## Receive test
`receive.js` is used to perform locally the listening action.
```bash
HUB_URL=amqps://messaging.integration.hub.esante.gouv.fr LRM_PASSPHRASE=... node receive.js 15-15_v1.5 fr.health.samuA.message
```

## Send test
`send.js` is used to perform locally the publish action.
```bash
# Using async call
HUB_URL=amqps://messaging.integration.hub.esante.gouv.fr LRM_PASSPHRASE=... node send.js async 15-15_v1.5 fr.health.samuA empty_test.json   
# Using callback call
HUB_URL=amqps://messaging.integration.hub.esante.gouv.fr LRM_PASSPHRASE=... node send.js cb 15-15_v1.5 fr.health.samuA empty_test.json   
```

## LRM back implem
The back-end of the LRM is not using `receive.js` or `send.js`!
However, it imports the same helpers from `util.js` that are used in the above scripts.
To change the way the back-end sends and receives messages, the scripts to update therefore are `utils.js` and `expressServer.js`. 
`receive.js` and `send.js` are used to perform small tests.
