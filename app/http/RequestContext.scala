package lila.app
package http

import play.api.mvc.*
import play.api.i18n.Lang
import play.api.http.Writeable

import lila.user.Me
import lila.api.{ Nonce, PageData, LoginContext }
import lila.i18n.I18nLangPicker
import lila.common.{ HTTPRequest }
import lila.security.{ Granter, FingerPrintedUser, AppealUser }
import lila.oauth.OAuthScope
import lila.pref.RequestPref

trait RequestContext(using Executor):

  val env: Env

  def makeContext(using req: RequestHeader): Fu[Context] = for
    userCtx <- makeUserContext(req)
    lang = getAndSaveLang(req, userCtx.me)
    pref <- env.pref.api.get(userCtx.me, req)
  yield Context(req, lang, userCtx, pref)

  def makeBodyContext[A](using req: Request[A]): Fu[BodyContext[A]] = for
    userCtx <- makeUserContext(req)
    lang = getAndSaveLang(req, userCtx.me)
    pref <- env.pref.api.get(userCtx.me, req)
  yield BodyContext(req, lang, userCtx, pref)

  def oauthContext(scoped: OAuthScope.Scoped)(using req: RequestHeader): Fu[Context] =
    val lang    = getAndSaveLang(req, scoped.me.some)
    val userCtx = LoginContext(scoped.me.some, false, none, scoped.scopes.some)
    env.pref.api
      .get(scoped.me, req)
      .map:
        Context(req, lang, userCtx, _)

  def oauthBodyContext[A](scoped: OAuthScope.Scoped)(using req: Request[A]): Fu[BodyContext[A]] =
    val lang    = getAndSaveLang(req, scoped.me.some)
    val userCtx = LoginContext(scoped.me.some, false, none, scoped.scopes.some)
    env.pref.api
      .get(scoped.me, req)
      .map:
        BodyContext(req, lang, userCtx, _)

  private def getAndSaveLang(req: RequestHeader, me: Option[Me]): Lang =
    val lang = I18nLangPicker(req, me.flatMap(_.lang))
    me.filter(_.lang.fold(true)(_ != lang.code)) foreach { env.user.repo.setLang(_, lang) }
    lang

  private def pageDataBuilder(using ctx: Context): Fu[PageData] =
    if HTTPRequest isSynchronousHttp ctx.req
    then
      val nonce = Nonce.random.some
      ctx.me.foldUse(fuccess(PageData.anon(nonce))): me ?=>
        env.user.lightUserApi preloadUser me
        val enabledId = me.enabled.yes option me.userId
        enabledId.so(env.team.api.nbRequests) zip
          enabledId.so(env.challenge.api.countInFor.get) zip
          enabledId.so(env.notifyM.api.unreadCount) zip
          env.mod.inquiryApi.forMod map { case (((teamNbRequests, nbChallenges), nbNotifications), inquiry) =>
            PageData(
              teamNbRequests,
              nbChallenges,
              nbNotifications,
              hasClas = env.clas.hasClas,
              inquiry = inquiry,
              nonce = nonce
            )
          }
    else fuccess(PageData.anon(none))

  def pageContext(using ctx: Context): Fu[PageContext] =
    pageDataBuilder.dmap(PageContext(ctx, _))

  def InEmbedContext[A](f: EmbedContext ?=> A)(using ctx: Context): A =
    f(using EmbedContext(ctx.req))

  private def makeUserContext(req: RequestHeader): Fu[LoginContext] =
    env.security.api restoreUser req dmap {
      case Some(Left(AppealUser(me))) if HTTPRequest.isClosedLoginPath(req) =>
        FingerPrintedUser(me, true).some
      case Some(Right(d)) if !env.net.isProd =>
        d.copy(me = d.me.map:
          _.addRole(lila.security.Permission.Beta.dbKey)
            .addRole(lila.security.Permission.Prismic.dbKey)
        ).some
      case Some(Right(d)) => d.some
      case _              => none
    } flatMap {
      case None => fuccess(LoginContext.anon)
      case Some(d) =>
        env.mod.impersonate.impersonating(d.me) map {
          _.fold(LoginContext(d.me.some, !d.hasFingerPrint, none, none)): impersonated =>
            LoginContext(Me(impersonated).some, needsFp = false, d.me.some, none)
        }
    }
