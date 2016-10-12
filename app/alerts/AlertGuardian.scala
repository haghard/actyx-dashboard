package alerts

import akka.actor._
import java.net.InetSocketAddress
import alerts.WsServer.Subscribe

object AlertGuardian {
  def props(conf: play.api.Configuration, bufferSize: Int) =
    Props(new AlertGuardian(conf, bufferSize))
}

class AlertGuardian(conf: play.api.Configuration, override val bufferSize: Int)
    extends Actor
    with ActorLogging
    with KafkaSupport {
  val cassandraPort = conf.getInt("cassandra-port").get
  val keySpace = conf.getString("cassandra-keyspace").get
  val cassandraHosts = conf.getString("cassandra").get.split(",").toSeq.map(new InetSocketAddress(_, cassandraPort))
  val query = conf.getString("cassandra-ma-query").get

  override val kafkaUrl = conf.getString("kafka.consumer.url").get
  override val kafkaTopic = conf.getString("kafka.consumer.topic").get
  override val clientId = conf.getString("kafka.consumer.client-id").get
  override val kafkaDispatcher = conf.getString("kafka.consumer.use-dispatcher").get
  override val aggregateTimeGapSec = conf.getInt("aggregate-max-gap-sec").get
  override val kafkaConsumerGroup = conf.getString("kafka.consumer.group").get

  val serverActor = context.actorOf(WsServer.props(self, bufferSize))

  override def preStart = runStreaming(serverActor)

  override def receive: Receive = {
    case s: Subscribe => serverActor forward s
    case th: Throwable =>
      log.error("Unrecoverable error", th)
      context.system.terminate()
  }
}
