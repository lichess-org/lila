package lila.app

// #TODO relocate that shit
// object Global extends GlobalSettings {

//   private val httpLogger = lila.log("http")

//   private def logHttp(code: Int, req: RequestHeader, exception: Option[Throwable] = None) = {
//     val message = s"$code ${HTTPRequest print req}"
//     exception match {
//       case Some(e) => httpLogger.warn(message, e)
//       case None => httpLogger.info(message)
//     }
//   }

//   override def onStart(app: Application): Unit = {
//     kamon.Kamon.start()
//     lila.app.Env.current
//   }

//   override def onStop(app: Application): Unit = {
//     kamon.Kamon.shutdown()
//   }

//   override def onRouteRequest(req: RequestHeader): Option[Handler] = {
//     lila.mon.http.request.all()
//     if (req.remoteAddress contains ":") lila.mon.http.request.ipv6()
//     if (HTTPRequest isXhr req) lila.mon.http.request.xhr()
//     else if (HTTPRequest isBot req) lila.mon.http.request.bot()
//     else lila.mon.http.request.page()

//     if (req.host != Env.api.Net.Domain &&
//       HTTPRequest.isRedirectable(req) &&
//       !HTTPRequest.isProgrammatic(req) &&
//       // asset request going through the CDN, don't redirect
//       !(req.host == Env.api.Net.AssetDomain && HTTPRequest.hasFileExtension(req)))
//       Some(Action(MovedPermanently(s"http${if (req.secure) "s" else ""}://${Env.api.Net.Domain}${req.uri}")))
//     else super.onRouteRequest(req) map {
//       case action: EssentialAction if HTTPRequest.isApiOrApp(req) => EssentialAction { r =>
//         action(r) map { _.withHeaders(ResponseHeaders.headersForApiOrApp(r): _*) }
//       }
//       case other => other
//     }
//   }

//   private def niceError(req: RequestHeader): Boolean =
//     req.method == "GET" &&
//       HTTPRequest.isSynchronousHttp(req) &&
//       !HTTPRequest.hasFileExtension(req)

//   override def onHandlerNotFound(req: RequestHeader) =
//     if (niceError(req)) {
//       logHttp(404, req)
//       controllers.Main.renderNotFound(req)
//     } else fuccess(NotFound("404 - Resource not found"))

//   override def onBadRequest(req: RequestHeader, error: String) = {
//     logHttp(400, req)
//     if (error startsWith "Illegal character in path") fuccess(Redirect("/"))
//     else if (error startsWith "Cannot parse parameter") onHandlerNotFound(req)
//     else if (niceError(req)) {
//       lila.mon.http.response.code400()
//       controllers.Lobby.handleStatus(req, Results.BadRequest)
//     } else fuccess(BadRequest(error))
//   }

//   override def onError(req: RequestHeader, ex: Throwable) = {
//     logHttp(500, req, ex.some)
//     if (niceError(req)) {
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
// }
