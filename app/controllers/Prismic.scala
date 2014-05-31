package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.ws._

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._

import io.prismic.{ Api => PrismicApi, _ }

object Prismic {

  private val ACCESS_TOKEN = "PRISMIC_TK"
  private val Cache = BuiltInCache(200)
  private val Logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic").debug(message)
    case 'ERROR => play.api.Logger("prismic").error(message)
    case _      => play.api.Logger("prismic").info(message)
  }

  private lazy val config = lila.common.PlayApp loadConfig "prismic"
  private lazy val prismicToken = scala.util.Try(config getString "token").toOption
  private lazy val apiUrl = config getString "api"

  def linkResolver(api: PrismicApi, ref: Option[String])(implicit request: RequestHeader) = DocumentLinkResolver(api) {
    case (Fragment.DocumentLink(id, docType, tags, slug, false), maybeBookmarked) =>
      routes.Blog.show(id, slug, ref).absoluteURL()
    case (link@Fragment.DocumentLink(_, _, _, _, true), _) =>
      routes.Lobby.home.absoluteURL()
  }

  // Compute the callback URL to use for the OAuth worklow
  // private def callbackUrl(implicit rh: RequestHeader) =
  //   routes.Prismic.callback(code = None, redirect_uri = rh.headers.get("referer")).absoluteURL()

  // -- A Prismic context that help to keep the reference to useful primisc.io contextual data
  case class Context(api: PrismicApi, ref: String, accessToken: Option[String], linkResolver: DocumentLinkResolver) {
    def maybeRef = Option(ref).filterNot(_ == api.master.ref)
    def hasPrivilegedAccess = accessToken.isDefined
  }

  // -- Build a Prismic context
  def buildContext(ref: Option[String])(implicit request: RequestHeader) =
    apiHome(request.session.get(ACCESS_TOKEN).orElse(prismicToken)) map { api =>
      Context(api,
        ref.map(_.trim).filterNot(_.isEmpty).getOrElse(api.master.ref),
        request.session.get(ACCESS_TOKEN),
        linkResolver(api, ref.filterNot(_ == api.master.ref))(request))
    }

  def WithPrismic(ref: Option[String] = None)(block: Context => Future[SimpleResult])(implicit req: RequestHeader): Future[SimpleResult] =
    buildContext(ref) flatMap block

  // -- Fetch the API entry document
  def apiHome(accessToken: Option[String] = None) =
    PrismicApi.get(apiUrl, accessToken = accessToken, cache = Cache, logger = Logger)

  // -- Helper: Retrieve a single document by Id
  def getDocument(id: String)(implicit ctx: Prismic.Context): Future[Option[Document]] =
    ctx.api.forms("everything")
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ctx.ref).submit() map (_.results.headOption)

  // -- Helper: Retrieve several documents by Id
  def getDocuments(ids: String*)(implicit ctx: Prismic.Context): Future[Seq[Document]] =
    ids match {
      case Nil => Future.successful(Nil)
      case ids => ctx.api.forms("everything")
        .query(s"""[[:d = any(document.id, ${ids.mkString("[\"", "\",\"", "\"]")})]]""")
        .ref(ctx.ref).submit() map (_.results)
    }

  // -- Helper: Retrieve a single document from its bookmark
  def getBookmark(bookmark: String)(implicit ctx: Prismic.Context): Future[Option[Document]] =
    ctx.api.bookmarks.get(bookmark).map(id => getDocument(id)).getOrElse(Future.successful(None))

  // --
  // -- OAuth actions
  // --

  // def signin = Action.async { implicit req =>
  //   apiHome().map(_.oauthInitiateEndpoint).recover {
  //     case InvalidToken(_, url)        => url
  //     case AuthorizationNeeded(_, url) => url
  //   } map { url =>
  //     Redirect(url, Map(
  //       "client_id" -> Seq(config("prismic.clientId")),
  //       "redirect_uri" -> Seq(callbackUrl),
  //       "scope" -> Seq("master+releases")
  //     ))
  //   }
  // }

  // def signout = Action {
  //   Redirect(routes.Application.index(ref = None)).withNewSession
  // }

  // def callback(code: Option[String], redirect_uri: Option[String]) =
  //   Action.async { implicit req =>
  //     (
  //       for {
  //         api <- apiHome()
  //         tokenResponse <- WS.url(api.oauthTokenEndpoint).post(Map(
  //           "grant_type" -> Seq("authorization_code"),
  //           "code" -> Seq(code.get),
  //           "redirect_uri" -> Seq(callbackUrl),
  //           "client_id" -> Seq(config("prismic.clientId")),
  //           "client_secret" -> Seq(config("prismic.clientSecret"))
  //         )).filter(_.status == 200).map(_.json)
  //       } yield {
  //         Redirect(redirect_uri.getOrElse(routes.Application.index(ref = None).url)).withSession(
  //           ACCESS_TOKEN -> (tokenResponse \ "access_token").as[String]
  //         )
  //       }
  //     ).recover {
  //         case x: Throwable =>
  //           Logger('ERROR, s"""Can't retrieve the OAuth token for code $code: ${x.getMessage}""".stripMargin)
  //           Unauthorized("Can't sign you in")
  //       }
  //   }

}
