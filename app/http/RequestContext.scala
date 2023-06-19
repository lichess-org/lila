package lila.app
package http

import play.api.mvc.*
import play.api.i18n.Lang

import lila.user.{ Me, UserContext }
import lila.api.{ Nonce, PageData }
import lila.i18n.I18nLangPicker
import lila.common.{ HTTPRequest }
import lila.security.{ Granter, FingerPrintedUser, AppealUser }
import lila.oauth.OAuthScope

trait RequestContext(using Executor):

  val env: Env

  def minimalContext(using req: RequestHeader): MinimalContext =
    MinimalContext(req, UserContext.anon)

  def minimalBodyContext[A](using req: Request[A]): MinimalBodyContext[A] =
    MinimalBodyContext(req, UserContext.anon)

  def webContext(using req: RequestHeader): Fu[WebContext] =
    restoreUser(req).flatMap: (d, impersonatedBy) =>
      val lang    = getAndSaveLang(req, d.map(_.me))
      val userCtx = UserContext(d.map(_.me), impersonatedBy)
      pageDataBuilder(d.exists(_.hasFingerPrint))(using req, userCtx).dmap:
        WebContext(req, lang, userCtx, _)

  def webBodyContext[A](using req: Request[A]): Fu[WebBodyContext[A]] =
    restoreUser(req).flatMap: (d, impersonatedBy) =>
      val lang    = getAndSaveLang(req, d.map(_.me))
      val userCtx = UserContext(d.map(_.me), impersonatedBy)
      pageDataBuilder(d.exists(_.hasFingerPrint))(using req, userCtx).dmap:
        WebBodyContext(req, lang, userCtx, _)

  def oauthContext(scoped: OAuthScope.Scoped)(using req: RequestHeader): OAuthContext =
    val lang    = getAndSaveLang(req, scoped.me.some)
    val userCtx = UserContext(scoped.me.some, none)
    OAuthContext(req, lang, userCtx, scoped.scopes)

  def oauthBodyContext[A](scoped: OAuthScope.Scoped)(using req: Request[A]): OAuthBodyContext[A] =
    val lang    = getAndSaveLang(req, scoped.me.some)
    val userCtx = UserContext(scoped.me.some, none)
    OAuthBodyContext(req, lang, userCtx, scoped.scopes)

  private def getAndSaveLang(req: RequestHeader, me: Option[Me]): Lang =
    val lang = I18nLangPicker(req, me.flatMap(_.lang))
    me.filter(_.lang.fold(true)(_ != lang.code)) foreach { env.user.repo.setLang(_, lang) }
    lang

  private def pageDataBuilder(
      hasFingerPrint: Boolean
  )(using req: RequestHeader, userCtx: UserContext): Fu[PageData] =
    val isPage = HTTPRequest isSynchronousHttp req
    val nonce  = isPage option Nonce.random
    userCtx.me.foldUse(fuccess(PageData.anon(req, nonce, isBlindMode))): me ?=>
      env.pref.api.getPref(me, req) zip {
        if isPage then
          env.user.lightUserApi preloadUser me
          val enabledId = me.enabled.yes option me.userId
          enabledId.so(env.team.api.nbRequests) zip
            enabledId.so(env.challenge.api.countInFor.get) zip
            enabledId.so(env.notifyM.api.unreadCount) zip
            env.mod.inquiryApi.forMod
        else
          fuccess:
            (((0, 0), lila.notify.Notification.UnreadCount(0)), none)
      } map { case (pref, (((teamNbRequests, nbChallenges), nbNotifications), inquiry)) =>
        PageData(
          teamNbRequests,
          nbChallenges,
          nbNotifications,
          pref,
          blindMode = isBlindMode,
          hasFingerprint = hasFingerPrint,
          hasClas = Granter(_.Teacher) || env.clas.studentCache.isStudent(me),
          inquiry = inquiry,
          nonce = nonce
        )
      }

  private def isBlindMode(using req: RequestHeader, userCtx: UserContext) =
    req.cookies.get(env.api.config.accessibility.blindCookieName) so { c =>
      c.value.nonEmpty && c.value == env.api.config.accessibility.hash(using userCtx.me)
    }

  // user, impersonatedBy
  private type RestoredUser = (Option[FingerPrintedUser], Option[Me])

  private def restoreUser(req: RequestHeader): Fu[RestoredUser] =
    env.security.api restoreUser req dmap {
      case Some(Left(AppealUser(user))) if HTTPRequest.isClosedLoginPath(req) =>
        FingerPrintedUser(user, true).some
      case Some(Right(d)) if !env.net.isProd =>
        d.copy(me = d.me.map:
          _.addRole(lila.security.Permission.Beta.dbKey)
            .addRole(lila.security.Permission.Prismic.dbKey)
        ).some
      case Some(Right(d)) => d.some
      case _              => none
    } flatMap {
      case None => fuccess(None -> None)
      case Some(d) =>
        env.mod.impersonate.impersonating(d.me) map {
          _.fold[RestoredUser](d.some -> None): impersonated =>
            FingerPrintedUser(Me(impersonated), hasFingerPrint = true).some -> d.me.some
        }
    }
