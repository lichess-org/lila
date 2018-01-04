package lila.api

import play.api.http.HeaderNames
import play.api.mvc._
import play.api.mvc.Results.InternalServerError

import lila.common.HTTPRequest

final class RequestDropper(
    websocketDropPercent: () => Int
) {

  def apply(req: RequestHeader): Option[Handler] = {
    websocketDropPercent() > 0 &&
      HTTPRequest.isSocket(req) &&
      scala.util.Random.nextInt(100) < websocketDropPercent()
  } option errorSocketHandler

  private val errorSocketHandler = Action(InternalServerError("Server rebooting, hang on..."))
}
