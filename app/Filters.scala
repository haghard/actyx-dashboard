import javax.inject._
import play.api._
import play.api.http.HttpFilters
import play.api.mvc._

import filters.ExampleFilter


@Singleton
class Filters @Inject() (
  env: Environment,
  exampleFilter: ExampleFilter) extends HttpFilters {


  /**
    Use the example filter if we're running development mode. If
    we're running in production or test mode then don't use any
    filters at all.
  */
  override val filters = {
    if (env.mode == Mode.Dev) Seq(exampleFilter) else Seq.empty
  }
}
