package lila.app
package http

import play.api.mvc.*
import play.api.i18n.Lang

import lila.api.context.*
import lila.user.{ User, UserContext, UserBodyContext }
import lila.api.{ Nonce, PageData }
import lila.i18n.I18nLangPicker
import lila.common.{ HTTPRequest }
import lila.security.Granter
import lila.security.FingerPrintedUser
import lila.security.AppealUser

trait RequestContext(using Executor):

  val env: Env

  def anonContext(request: RequestHeader): AnyContext = new:
    val userContext = UserContext(request, none, none, getAndSaveLang(request, none))

  def anonBodyContext[A](request: Request[A]): BodyContext[A] = new:
    val userContext = UserContext(request, none, none, getAndSaveLang(request, none))
    def body        = request

  def webContext(req: RequestHeader): Fu[WebContext] =
    restoreUser(req).flatMap: (d, impersonatedBy) =>
      val lang = getAndSaveLang(req, d.map(_.user))
      val ctx  = UserContext(req, d.map(_.user), impersonatedBy, lang)
      pageDataBuilder(d.exists(_.hasFingerPrint))(using ctx) dmap { WebContext(ctx, _) }

  def webBodyContext[A](req: Request[A]): Fu[WebBodyContext[A]] =
    restoreUser(req).flatMap: (d, impersonatedBy) =>
      val lang = getAndSaveLang(req, d.map(_.user))
      val ctx  = UserBodyContext(req, d.map(_.user), impersonatedBy, lang)
      pageDataBuilder(d.exists(_.hasFingerPrint))(using ctx) dmap { WebBodyContext(ctx, _) }

  def getAndSaveLang(req: RequestHeader, user: Option[User]): Lang =
    val lang = I18nLangPicker(req, user.flatMap(_.lang))
    user.filter(_.lang.fold(true)(_ != lang.code)) foreach { env.user.repo.setLang(_, lang) }
    lang

  private def pageDataBuilder(hasFingerPrint: Boolean)(using ctx: UserContext): Fu[PageData] =
    val isPage = HTTPRequest isSynchronousHttp ctx.req
    val nonce  = isPage option Nonce.random
    ctx.me.fold(fuccess(PageData.anon(ctx.req, nonce, isBlindMode))): me =>
      env.pref.api.getPref(me, ctx.req) zip {
        if isPage then
          env.user.lightUserApi preloadUser me
          val enabledId = me.enabled.yes option me.id
          enabledId.so(env.team.api.nbRequests) zip
            enabledId.so(env.challenge.api.countInFor.get) zip
            enabledId.so(env.notifyM.api.unreadCount) zip
            env.mod.inquiryApi.forMod(me)
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
          hasClas = Granter(_.Teacher)(me) || env.clas.studentCache.isStudent(me.id),
          inquiry = inquiry,
          nonce = nonce
        )
      }

  private def isBlindMode(using ctx: UserContext) =
    ctx.req.cookies.get(env.api.config.accessibility.blindCookieName) so { c =>
      c.value.nonEmpty && c.value == env.api.config.accessibility.hash
    }

  // user, impersonatedBy
  private type RestoredUser = (Option[FingerPrintedUser], Option[User])

  private def restoreUser(req: RequestHeader): Fu[RestoredUser] =
    env.security.api restoreUser req dmap {
      case Some(Left(AppealUser(user))) if HTTPRequest.isClosedLoginPath(req) =>
        FingerPrintedUser(user, true).some
      case Some(Right(d)) if !env.net.isProd =>
        d.copy(user =
          d.user
            .addRole(lila.security.Permission.Beta.dbKey)
            .addRole(lila.security.Permission.Prismic.dbKey)
        ).some
      case Some(Right(d)) => d.some
      case _              => none
    } flatMap {
      case None => fuccess(None -> None)
      case Some(d) =>
        env.mod.impersonate.impersonating(d.user) map {
          _.fold[RestoredUser](d.some -> None): impersonated =>
            FingerPrintedUser(impersonated, hasFingerPrint = true).some -> d.user.some
        }
    }
