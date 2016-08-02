package controllers

import javax.inject._
import play.api.libs.json.{JsObject, JsString, JsNumber, Json}
import play.api.mvc._
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._
import scala.concurrent.Future

object SpotsController {
  case class Location(lat: Double, long: Double)
  case class DeviceSpot(name: String, location: Location)
}

@Singleton
class SpotsController @Inject()(val conf: play.api.Configuration)(
    implicit ec: ExecutionContext)
    extends Controller {
  import SpotsController._

  def index = Action { implicit request =>
    Ok(views.html.spotsSimple(conf.underlying.getString("GMAPS_API_KEY")))
  }

  def sport = Action.async { implicit request =>
    val jsSpots = conf.underlying.getObjectList("devices").asScala.map { cfg =>
      val local = cfg.toConfig
      val spot =
        DeviceSpot(local.getString("id"),
                   Location(local.getDouble("lat"), local.getDouble("lon")))
      JsObject(
          Seq(
              "name" -> JsString(spot.name),
              "description" -> JsString(""),
              "location" -> JsObject(
                  Seq("latitude" -> JsNumber(spot.location.lat),
                      "longitude" -> JsNumber(spot.location.long)))
          ))
    }
    Future.successful(Ok(Json.arr(jsSpots)))
  }
}
