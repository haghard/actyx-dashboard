actyx-dashboard
===============

##  Run with docker


docker run --net="host" -d -p 9000:9000 haghard/actyx-dashboard:0.0.1 -Dakka.remote.netty.tcp.port=... -Dakka.remote.netty.tcp.hostname=... -Dkafka.consumer.url=... -Dcassandra=... -Dakka.cluster.seed=... -DGMAPS_API_KEY=...