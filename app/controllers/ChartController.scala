package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

@Singleton
class ChartController @Inject()(val conf: play.api.Configuration)(
    implicit system: ActorSystem,
    mat: Materializer,
    ec: ExecutionContext)
    extends Controller {
  def index = Action { implicit request =>
    val devices = conf.underlying
      .getObjectList("devices")
      .asScala
      .map { cfg =>
        cfg.toConfig.getString("id")
      }
      .toSet
    Ok(views.html.pieChart(devices))
  }
}
