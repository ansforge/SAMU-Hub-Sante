#!/bin/bash

# Import valid and invalid strings from files
BATCH_SIZE=$1
VALID=$2
INVALID=$3
ROUTING_KEY=$4

# Loop logic
rabbitmqadmin -u admin -p admin \


START_TIME=$(date +'%s.%N')
for ((i = 0; i < $BATCH_SIZE/10; i++)); do
    for ((j = 1; j < 11; j++)); do
        payload=$([ $j -lt 10 ] && echo "$VALID" || echo "$INVALID")

        rabbitmqadmin -u admin -p admin publish \
          exchange=hubsante \
          properties='{"content_type":"application/json","delivery_mode":2}' \
          routing_key="$ROUTING_KEY" \
          payload="$payload"
    done
done
END_TIME=$(date +'%s.%N')