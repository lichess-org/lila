package lila.app
package http

import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing._
import play.api.{ Configuration, Environment, UsefulException }
import play.core.SourceMapper
import scala.concurrent.Future

import lila.common.HTTPRequest

final class ErrorHandler(
    environment: Environment,
    config: Configuration,
    sourceMapper: Option[SourceMapper],
    router: => Option[Router],
    keyPages: => controllers.KeyPages
) extends DefaultHttpErrorHandler(environment, config, sourceMapper, router) {

  private lazy val httpLogger = lila.log("http")

  override def onProdServerError(req: RequestHeader, exception: UsefulException) = Future {
    val handlerDef: HandlerDef = req.attrs(Router.Attrs.HandlerDef)
    val action = s"${handlerDef.controller}.${handlerDef.method}"
    httpLogger.error(s"ERROR 500 $action", exception)
    lila.mon.http.response.code500()
    if (canShowErrorPage(req))
      InternalServerError(views.html.base.errorPage(exception) {
        lila.api.Context.error(
          req,
          lila.i18n.defaultLang,
          HTTPRequest.isSynchronousHttp(req) option lila.common.Nonce.random
        )
      })
    else InternalServerError(exception.getMessage)
  } recover {
    case util.control.NonFatal(e) =>
      httpLogger.error(s"""Error handler exception on "${exception.getMessage}\"""", e)
      InternalServerError("Sorry, something went very wrong.")
  }

  private def canShowErrorPage(req: RequestHeader): Boolean =
    HTTPRequest.isSynchronousHttp(req) && !HTTPRequest.hasFileExtension(req)

  override def onNotFound(req: RequestHeader, msg: String) =
    if (canShowErrorPage(req)) keyPages.notFound(req)
    else fuccess(NotFound("404 - Resource not found"))

  //   override def onBadRequest(req: RequestHeader, error: String) = {
  //     logHttp(400, req)
  //     if (error startsWith "Illegal character in path") fuccess(Redirect("/"))
  //     else if (error startsWith "Cannot parse parameter") onHandlerNotFound(req)
  //     else if (canShowErrorPage(req)) {
  //       lila.mon.http.response.code400()
  //       controllers.Lobby.handleStatus(req, Results.BadRequest)
  //     } else fuccess(BadRequest(error))
  //   }

  //   override def onError(req: RequestHeader, ex: Throwable) = {
  //     logHttp(500, req, ex.some)
  //     if (canShowErrorPage(req)) {
  //       if (lila.common.PlayApp.isProd) {
  //         lila.mon.http.response.code500()
  //         fuccess(InternalServerError(views.html.base.errorPage(ex) {
  //           lila.api.Context.error(
  //             req,
  //             lila.i18n.defaultLang,
  //             HTTPRequest.isSynchronousHttp(req) option lila.common.Nonce.random
  //           )
  //         }))
  //       } else super.onError(req, ex)
  //     } else scala.concurrent.Future {
  //       InternalServerError(ex.getMessage)
  //     } recover {
  //       // java.lang.NullPointerException: null
  //       // at play.api.mvc.Codec$$anonfun$javaSupported$1.apply(Results.scala:320) ~[com.typesafe.play.play_2.11-2.4.11.jar:2.4.11]
  //       case e: java.lang.NullPointerException =>
  //         httpLogger.warn(s"""error handler exception on "${ex.getMessage}\"""", e)
  //         InternalServerError("Something went wrong.")
  //     }
  //   }
}
