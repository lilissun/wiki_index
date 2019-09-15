#!/bin/bash

solr delete -c wiki

sleep 10

solr create -c wiki -s 1 -rf 1

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"title", "type":"text_en", "stored":true, "indexed": true, "multiValued":false, "required": true}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"path", "type":"string", "stored":true, "indexed": true, "multiValued":true, "required": false}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"tags", "type":"string", "stored":true, "indexed": true, "multiValued":true, "required": false}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"tokens", "type":"text_general", "stored":true, "indexed": true, "multiValued":false, "required": false}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"title_exact", "type":"string", "stored":true, "indexed": true, "multiValued":false, "required": false}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"tokens_exact", "type":"string", "stored":true, "indexed": true, "multiValued":false, "required": false}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"title_to_lower_exact", "type":"string", "stored":true, "indexed": true, "multiValued":false, "required": false}}' \
  http://localhost:8983/solr/wiki/schema

curl -X POST -H 'Content-type:application/json' \
  --data-binary '{"add-field": {"name":"entities", "type":"string", "stored":true, "indexed": true, "multiValued":true, "required": false}}' \
  http://localhost:8983/solr/wiki/schema
