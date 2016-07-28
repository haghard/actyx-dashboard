import akka.cluster.Cluster
import com.google.inject.AbstractModule
import java.time.Clock

import play.api.{Configuration, Environment}
import play.api.libs.concurrent.AkkaGuiceSupport
import services.ApplicationInitializer

class Module(environment: Environment, conf: Configuration)
    extends AbstractModule
    with AkkaGuiceSupport {

  override def configure() = {
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    bind(classOf[ApplicationInitializer]).asEagerSingleton()
  }
}
