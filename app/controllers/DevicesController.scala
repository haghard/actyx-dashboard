package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

@Singleton
class DevicesController @Inject()(val conf: play.api.Configuration)(
    implicit ec: ExecutionContext)
    extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.devices(conf.getStringList("devices").get.asScala.toSeq))
  }
}
