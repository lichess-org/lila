package lila.app

import akka.stream.Materializer
import play.api.Logging
import play.api.mvc._
import play.api.routing._

final class LilaHttpFilter(env: Env)(implicit val mat: Materializer) extends Filter with Logging {

  def apply(nextFilter: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] = {

    val startTime = nowMillis

    nextFilter(req).map { result =>
      val handlerDef: HandlerDef = req.attrs(Router.Attrs.HandlerDef)
      val action = s"${handlerDef.controller}.${handlerDef.method}"
      val reqTime = nowMillis - startTime

      if (env.isDev)
        logger.info(s"${action} took ${reqTime}ms and returned ${result.header.status}")
      else
        lila.mon.http.time(action)(reqTime)

      result
    }
  }
}
