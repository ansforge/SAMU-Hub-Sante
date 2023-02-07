#!/bin/bash
set -x
for f in ./NexSIS/xsd-cisu/*
do
  name=${f##*/}
  echo $name
  echo $f
  node index.js "$f" > "./NexSIS/json-cisu/$name.json"
done