package alerts

import akka.actor.Actor
import com.datastax.driver.core._
import java.net.InetSocketAddress
import com.github.levkhomich.akka.tracing.{ActorTracing, TracingActorLogging}

import scala.collection.JavaConverters._

trait CassandraSupport extends TracingActorLogging with ActorTracing {
  mixin: Actor =>

  def cassandraHosts: Seq[InetSocketAddress]

  def cassandraClient(cl: ConsistencyLevel): Cluster =
    Cluster.builder()
      .addContactPointsWithPorts(cassandraHosts.asJava)
      .withQueryOptions(new QueryOptions().setConsistencyLevel(cl))
      .build

}