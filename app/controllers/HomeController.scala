package controllers

import ui.HtmlReader
import javax.inject._
import play.api.mvc._
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(ac: AlertsController,
                               dc: DevicesController,
                               cc: ChartController)(
    implicit as: ActorSystem,
    mat: akka.stream.Materializer,
    ec: ExecutionContext)
    extends Controller
    with HtmlReader {

  def index = Action.async { implicit request =>
    for {
      alerts <- ac.index(request)
      devices <- dc.index(request)
      chart <- cc.index(request)
      alertsHtml <- readBody(alerts)
      devicesHtml <- readBody(devices)
      chartHtml <- readBody(chart)
    } yield Ok(views.html.index(alertsHtml, devicesHtml, chartHtml))
  }
}
