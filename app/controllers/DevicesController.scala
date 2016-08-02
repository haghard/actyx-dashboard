package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

@Singleton
class DevicesController @Inject()(val conf: play.api.Configuration)(
    implicit ec: ExecutionContext)
    extends Controller {

  def index = Action { implicit request =>
    val ids = conf.underlying.getObjectList("devices").asScala.toSeq.map {
      _.toConfig.getString("id")
    }
    Ok(views.html.devices(ids))
  }
}
