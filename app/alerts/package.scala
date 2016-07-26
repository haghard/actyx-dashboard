import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.{Promise, Future}

package object alerts {

  implicit class DoubleOpts(n: Double) {
    def scale(x: Int) = {
      val w = Math.pow(10, x)
      (n * w).toLong.toDouble / w
    }
  }

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

  case class Reading(deviceId: String,
                     current: Double,
                     threshold: Double,
                     when: ZonedDateTime,
                     name: String,
                     `type`: String,
                     state: String)

  implicit val reads: Reads[Reading] = (
      (JsPath \ "device_id").read[String] and (JsPath \ "current")
        .read[Double] and (JsPath \ "current_alert").read[Double]
        and (JsPath \ "timestamp")
          .read[String]
          .map(ZonedDateTime.parse(_, formatter))
        and (JsPath \ "name").read[String] and (JsPath \ "type")
        .read[String] and (JsPath \ "state").read[String]
  )(Reading.apply _)

  implicit class FutureOpts[T](lf: ListenableFuture[T]) {
    def asScala: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable) = p failure t
        def onSuccess(result: T) = p success result
      })
      p.future
    }
  }
}
