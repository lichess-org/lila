package controllers

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest

object Options extends LilaController {

  val root = all("")

  def all(url: String) = Action { req =>
    if (isLocalhost(req) || isApi(req)) {
      val methods = getMethods(req)
      if (methods.nonEmpty) {
        val allow = ("OPTIONS" :: methods).mkString(", ")
        NoContent.withHeaders(
          List(
            "Allow" -> allow,
            "Access-Control-Allow-Methods" -> allow,
            "Access-Control-Allow-Origin" -> { if (isLocalhost(req)) localhost else "*" },
            "Access-Control-Allow-Headers" -> "Origin,Authorization",
            "Access-Control-Max-Age" -> "1728000",
            "Vary" -> "Origin"
          ): _*
        )
      } else
        NotFound
    } else
      NotFound
  }

  private val localhost = "http://localhost:8080"

  private def isLocalhost(req: RequestHeader) = HTTPRequest.origin(req) has localhost
  private def isApi(req: RequestHeader) = req.uri startsWith "/api/"

  private val cache: Cache[String, List[String]] = Scaffeine()
    .maximumSize(8192)
    .build[String, List[String]]

  private val methodList = List("GET", "POST")

  private lazy val router = lila.common.PlayApp.router

  private def getMethods(req: RequestHeader): List[String] =
    cache.get(req.uri, uri =>
      methodList.filter { m =>
        router.handlerFor(req.copy(method = m)).isDefined
      })
}
