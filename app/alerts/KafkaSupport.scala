package alerts

import akka.kafka.{ Subscriptions, ConsumerSettings}
import akka.kafka.scaladsl.Consumer
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{ Source, Sink}
import java.time.format.DateTimeFormatter
import com.datastax.driver.core.ConsistencyLevel
import akka.actor.{ ActorRef, ActorLogging, Actor}
import org.apache.kafka.clients.consumer.ConsumerConfig
import akka.stream._
//import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.libs.json.JsResultException
import play.api.libs.json._

import scala.concurrent.Future

trait KafkaSupport extends CassandraSupport {
  mixin: Actor with ActorLogging =>

  def kafkaUrl: String

  def kafkaTopic: String

  def clientId: String

  def kafkaDispatcher: String

  def bufferSize: Int

  def keySpace: String

  def query: String

  def aggregateTimeGapSec: Int

  def kafkaConsumerGroup: String

  val retryTimeout = 5000

  val tbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def consumerSettings = ConsumerSettings(context.system.settings.config.getConfig("kafka.consumer"), new StringDeserializer(), new StringDeserializer())
    .withBootstrapServers(kafkaUrl)
    .withGroupId(kafkaConsumerGroup)
    .withClientId(clientId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withDispatcher(kafkaDispatcher)
  //Subscriptions.assignment(new TopicPartition("topic", 2)

  private val decider: Supervision.Decider = {
    case ex: JsResultException =>
      log.error(ex, "Ignore deserialization json error")
      akka.stream.Supervision.Resume
    case ex: Throwable =>
      log.error(ex, "Unexpected error")
      Thread.sleep(retryTimeout)
      akka.stream.Supervision.Resume
  }

  //at-most-once
  def runStreaming(actor: ActorRef): Unit = {
    val materializer = ActorMaterializer(
      ActorMaterializerSettings(context.system)
        .withInputBuffer(bufferSize, bufferSize)
        .withDispatcher(kafkaDispatcher)
        .withSupervisionStrategy(decider))(context.system)

    implicit val ex = materializer.executionContext
    Source.single {
      val session = (cassandraClient(ConsistencyLevel.LOCAL_ONE) connect keySpace)
      (session, (session prepare query))
    }.flatMapConcat { args =>
      Consumer.committableSource(consumerSettings, Subscriptions.topics(kafkaTopic)).mapAsync(4) { message => //parallelism should be configurable
        for {
          queryParams <- Future {
            val r = Json.parse(message.value).as[Reading]
            val timeBucket = (tbFormatter format r.when)
            val eventTime = r.when
            //Thread.sleep(100)
            //log.debug(r.deviceId)
            (r.deviceId, timeBucket, eventTime, r.current, r.threshold)
          }

          //To query db on every alert isn't efficient so that we can cache them
          result <- args._1.executeAsync(args._2.bind(queryParams._1, queryParams._2,
            Long.box(queryParams._3.minusSeconds(aggregateTimeGapSec).toInstant.toEpochMilli),
            Long.box(queryParams._3.toInstant.toEpochMilli)
            //Date.from(queryParams._3.minusSeconds(aggregateTimeGapSec).toInstant),
            //Date.from(queryParams._3.toInstant))
          )).asScala

          _ <- message.committableOffset.commitScaladsl
        } yield {
          //if(ThreadLocalRandom.current.nextDouble(0.0, 1.0) > .9) throw new Exception("Cassandra error")
          Option(result.one).fold (
            Json.obj("deviceId" -> queryParams._1, "ma" -> "none", "when" -> (formatter format queryParams._3), "actlimit" -> s"${queryParams._4}:${queryParams._5}")) { row =>
            Json.obj("deviceId" -> queryParams._1, "ma" -> row.getDouble("value").scale(3), "when" -> (formatter format queryParams._3), "actlimit" -> s"${queryParams._4}:${queryParams._5}")
          }
        }
      }
    }.to(Sink.fromSubscriber(ActorSubscriber(actor))).run()(materializer)
      //.runWith(Sink.actorRef(self, Status.Failure(new Exception(""))))
  }
}