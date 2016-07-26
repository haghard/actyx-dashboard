package ui

import play.twirl.api.Html
import scala.concurrent.Future
import play.api.mvc.Result

//https://www.playframework.com/documentation/2.5.x/StreamsMigration25#Migrating-Enumerators-to-Sources
trait HtmlReader {
  def readBody(result: Result)(implicit mat: akka.stream.Materializer): Future[Html] =
    result.body.consumeData.map(bytes => Html(bytes.utf8String))(mat.executionContext)
}