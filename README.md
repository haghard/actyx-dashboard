actyx-dashboard
===============

The config contains a filter for the devices to be shown in UI

##  Run with docker


docker run --net="host" -it -p 9000:9000 haghard/actyx-dashboard:0.0.1 -Dkafka.consumer.url=... -Dcassandra=...