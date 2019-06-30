package controllers

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.chat.Chat
import lila.common.{ IpAddress, EmailAddress, HTTPRequest }
import lila.report.{ Suspect, Mod => AsMod, SuspectId }
import lila.security.Permission
import lila.user.{ UserRepo, User => UserModel, Title }
import lila.mod.UserSearch
import ornicar.scalalib.Zero
import views._

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import scala.concurrent.duration._

object Mod extends LilaController {

  private def modApi = Env.mod.api
  private def modLogApi = Env.mod.logApi
  private def assessApi = Env.mod.assessApi

  def engine(username: String, v: Boolean) = OAuthModBody(_.MarkEngine) { me =>
    withSuspect(username) { sus =>
      for {
        inquiry <- Env.report.api.inquiries ofModId me.id
        _ <- modApi.setEngine(AsMod(me), sus, v)
      } yield (inquiry, sus).some
    }
  }(ctx => me => {
    case (inquiry, suspect) => Report.onInquiryClose(inquiry, me, suspect.some)(ctx)
  })

  def publicChat = Secure(_.ChatTimeout) { implicit ctx => _ =>
    Env.mod.publicChat.all map {
      case (tournamentsAndChats, simulsAndChats) =>
        Ok(html.mod.publicChat(tournamentsAndChats, simulsAndChats))
    }
  }

  def booster(username: String, v: Boolean) = OAuthModBody(_.MarkBooster) { me =>
    withSuspect(username) { prev =>
      for {
        inquiry <- Env.report.api.inquiries ofModId me.id
        suspect <- modApi.setBooster(AsMod(me), prev, v)
      } yield (inquiry, suspect).some
    }
  }(ctx => me => {
    case (inquiry, suspect) => Report.onInquiryClose(inquiry, me, suspect.some)(ctx)
  })

  def troll(username: String, v: Boolean) = OAuthModBody(_.Shadowban) { me =>
    withSuspect(username) { prev =>
      for {
        inquiry <- Env.report.api.inquiries ofModId me.id
        suspect <- modApi.setTroll(AsMod(me), prev, v)
      } yield (inquiry, suspect).some
    }
  }(ctx => me => {
    case (inquiry, suspect) => Report.onInquiryClose(inquiry, me, suspect.some)(ctx)
  })

  def warn(username: String, subject: String) = OAuthModBody(_.ModMessage) { me =>
    lila.message.ModPreset.bySubject(subject) ?? { preset =>
      withSuspect(username) { prev =>
        for {
          inquiry <- Env.report.api.inquiries ofModId me.id
          suspect <- modApi.setTroll(AsMod(me), prev, prev.user.troll)
          thread <- Env.message.api.sendPreset(me, suspect.user, preset)
          _ <- Env.mod.logApi.modMessage(thread.creatorId, thread.invitedId, thread.name)
        } yield (inquiry, suspect).some
      }
    }
  }(ctx => me => {
    case (inquiry, suspect) => Report.onInquiryClose(inquiry, me, suspect.some)(ctx)
  })

  def ban(username: String, v: Boolean) = OAuthMod(_.IpBan) { _ => me =>
    withSuspect(username) { sus =>
      modApi.setBan(AsMod(me), sus, v) map some
    }
  }(actionResult(username))

  def deletePmsAndChats(username: String) = OAuthMod(_.Shadowban) { _ => me =>
    withSuspect(username) { sus =>
      Env.mod.publicChat.delete(sus) >>
        Env.message.api.deleteThreadsBy(sus.user) map some
    }
  }(actionResult(username))

  def disableTwoFactor(username: String) = Secure(_.DisableTwoFactor) { implicit ctx => me =>
    modApi.disableTwoFactor(me.id, username) >> User.modZoneOrRedirect(username, me)
  }

  def closeAccount(username: String) = OAuthMod(_.CloseAccount) { _ => me =>
    modApi.closeAccount(me.id, username).flatMap {
      _.?? { user =>
        Env.current.closeAccount(user.id, self = false) map some
      }
    }
  }(actionResult(username))

  def reopenAccount(username: String) = OAuthMod(_.ReopenAccount) { _ => me =>
    modApi.reopenAccount(me.id, username) map some
  }(actionResult(username))

  def reportban(username: String, v: Boolean) = OAuthMod(_.ReportBan) { _ => me =>
    withSuspect(username) { sus =>
      modApi.setReportban(AsMod(me), sus, v) map some
    }
  }(actionResult(username))

  def rankban(username: String, v: Boolean) = OAuthMod(_.RemoveRanking) { _ => me =>
    withSuspect(username) { sus =>
      modApi.setRankban(AsMod(me), sus, v) map some
    }
  }(actionResult(username))

  def impersonate(username: String) = Auth { implicit ctx => me =>
    if (username == "-" && lila.mod.Impersonate.isImpersonated(me)) fuccess {
      lila.mod.Impersonate.stop(me)
      Redirect(routes.User.show(me.username))
    }
    else if (isGranted(_.Impersonate)) OptionFuRedirect(UserRepo named username) { user =>
      lila.mod.Impersonate.start(me, user)
      fuccess(routes.User.show(user.username))
    }
    else notFound
  }

  def setTitle(username: String) = SecureBody(_.SetTitle) { implicit ctx => me =>
    implicit def req = ctx.body
    lila.user.DataForm.title.bindFromRequest.fold(
      err => fuccess(redirect(username, mod = true)),
      title => modApi.setTitle(me.id, username, title map Title.apply) >>
        Env.security.automaticEmail.onTitleSet(username) >>-
        Env.user.uncacheLightUser(UserModel normalize username) inject
        redirect(username, mod = false)
    )
  }

  def setEmail(username: String) = SecureBody(_.SetEmail) { implicit ctx => me =>
    implicit def req = ctx.body
    OptionFuResult(UserRepo named username) { user =>
      Env.security.forms.modEmail(user).bindFromRequest.fold(
        err => BadRequest(err.toString).fuccess,
        rawEmail => {
          val email = Env.security.emailAddressValidator.validate(EmailAddress(rawEmail)) err s"Invalid email ${rawEmail}"
          modApi.setEmail(me.id, user.id, email.acceptable) inject redirect(user.username, mod = true)
        }
      )
    }
  }

  def notifySlack(username: String) = OAuthMod(_.ModNote) { _ => me =>
    withSuspect(username) { sus =>
      Env.slack.api.userMod(user = sus.user, mod = me) map some
    }
  }(actionResult(username))

  def log = Secure(_.ModLog) { implicit ctx => me =>
    modLogApi.recent map { html.mod.log(_) }
  }

  private def communications(username: String, priv: Boolean) = Secure {
    perms => if (priv) perms.ViewPrivateComms else perms.Shadowban
  } { implicit ctx => me =>
    OptionFuOk(UserRepo named username) { user =>
      lila.game.GameRepo.recentPovsByUserFromSecondary(user, 80) flatMap { povs =>
        priv.?? {
          Env.chat.api.playerChat optionsByOrderedIds povs.map(_.gameId).map(Chat.Id.apply)
        } zip
          priv.?? {
            lila.message.ThreadRepo.visibleOrDeletedByUser(user.id, 60).map {
              _ filter (_ hasPostsWrittenBy user.id) take 30
            }
          } zip
          (Env.shutup.api getPublicLines user.id) zip
          (Env.security userSpy user) zip
          Env.user.noteApi.forMod(user.id) zip
          Env.mod.logApi.userHistory(user.id) zip
          Env.report.api.inquiries.ofModId(me.id) map {
            case chats ~ threads ~ publicLines ~ spy ~ notes ~ history ~ inquiry =>
              if (priv && !inquiry.??(_.isRecentCommOf(Suspect(user))))
                Env.slack.api.commlog(mod = me, user = user, inquiry.map(_.oldestAtom.by.value))
              val povWithChats = (povs zip chats) collect {
                case (p, Some(c)) if c.nonEmpty => p -> c
              } take 15
              val filteredNotes = notes.filter(_.from != "irwin")
              html.mod.communication(user, povWithChats, threads, publicLines, spy, filteredNotes, history, priv)
          }
      }
    }
  }

  def communicationPublic(username: String) = communications(username, false)
  def communicationPrivate(username: String) = communications(username, true)

  def ipIntel(ip: String) = Secure(_.IpBan) { ctx => me =>
    Env.security.ipIntel.failable(IpAddress(ip)).map { Ok(_) }.recover {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  protected[controllers] def redirect(username: String, mod: Boolean = true) =
    Redirect(routes.User.show(username).url + mod.??("?mod"))

  def refreshUserAssess(username: String) = Secure(_.MarkEngine) { implicit ctx => me =>
    OptionFuResult(UserRepo named username) { user =>
      assessApi.refreshAssessByUsername(username) >>
        Env.irwin.api.requests.fromMod(Suspect(user), AsMod(me)) >>
        User.renderModZoneActions(username)
    }
  }

  def spontaneousInquiry(username: String) = Secure(_.SeeReport) { implicit ctx => me =>
    OptionFuResult(UserRepo named username) { user =>
      Env.report.api.inquiries.spontaneous(AsMod(me), Suspect(user)) inject redirect(user.username, true)
    }
  }

  def gamify = Secure(_.SeeReport) { implicit ctx => me =>
    Env.mod.gamify.leaderboards zip
      Env.mod.gamify.history(orCompute = true) map {
        case (leaderboards, history) => Ok(html.mod.gamify.index(leaderboards, history))
      }
  }
  def gamifyPeriod(periodStr: String) = Secure(_.SeeReport) { implicit ctx => me =>
    lila.mod.Gamify.Period(periodStr).fold(notFound) { period =>
      Env.mod.gamify.leaderboards map { leaderboards =>
        Ok(html.mod.gamify.period(leaderboards, period))
      }
    }
  }

  def search = SecureBody(_.UserSearch) { implicit ctx => me =>
    implicit def req = ctx.body
    val f = UserSearch.form
    f.bindFromRequest.fold(
      err => BadRequest(html.mod.search(err, Nil)).fuccess,
      query => Env.mod.search(query) map { html.mod.search(f.fill(query), _) }
    )
  }

  protected[controllers] def searchTerm(q: String)(implicit ctx: Context) = {
    val query = UserSearch exact q
    Env.mod.search(query) map { users => Ok(html.mod.search(UserSearch.form fill query, users)) }
  }

  def chatUser(username: String) = Secure(_.ChatTimeout) { implicit ctx => me =>
    implicit val lightUser = Env.user.lightUserSync _
    JsonOptionOk {
      Env.chat.api.userChat userModInfo username map2 lila.chat.JsonView.userModInfo
    }
  }

  def permissions(username: String) = Secure(_.ChangePermission) { implicit ctx => me =>
    OptionOk(UserRepo named username) { user =>
      html.mod.permissions(user)
    }
  }

  def savePermissions(username: String) = SecureBody(_.ChangePermission) { implicit ctx => me =>
    implicit def req = ctx.body
    import lila.security.Permission
    OptionFuResult(UserRepo named username) { user =>
      Form(single(
        "permissions" -> list(text.verifying { str =>
          Permission.allButSuperAdmin.exists(_.name == str)
        })
      )).bindFromRequest.fold(
        err => BadRequest(html.mod.permissions(user)).fuccess,
        permissions =>
          modApi.setPermissions(AsMod(me), user.username, Permission(permissions)) >> {
            (Permission(permissions) diff Permission(user.roles) contains Permission.Coach) ??
              Env.security.automaticEmail.onBecomeCoach(user)
          } >> {
            Permission(permissions).exists(_ is Permission.SeeReport) ?? Env.plan.api.setLifetime(user)
          } inject redirect(user.username, true)
      )
    }
  }

  def emailConfirm = SecureBody(_.SetEmail) { implicit ctx => me =>
    get("q") match {
      case None => Ok(html.mod.emailConfirm("", none, none)).fuccess
      case Some(rawQuery) =>
        val query = rawQuery.trim.split(' ').toList
        val email = query.headOption.map(EmailAddress.apply) flatMap Env.security.emailAddressValidator.validate
        val username = query lift 1
        def tryWith(setEmail: EmailAddress, q: String): Fu[Option[Result]] =
          Env.mod.search(UserSearch.exact(q)) flatMap {
            case List(UserModel.WithEmails(user, _)) => (!user.everLoggedIn).?? {
              lila.mon.user.register.modConfirmEmail()
              modApi.setEmail(me.id, user.id, setEmail)
            } >>
              UserRepo.email(user.id) map { email =>
                Ok(html.mod.emailConfirm("", user.some, email)).some
              }
            case _ => fuccess(none)
          }
        email.?? { em =>
          tryWith(em.acceptable, em.acceptable.value) orElse {
            username ?? { tryWith(em.acceptable, _) }
          }
        } getOrElse BadRequest(html.mod.emailConfirm(rawQuery, none, none)).fuccess
    }
  }

  def chatPanic = Secure(_.Shadowban) { implicit ctx => me =>
    Ok(html.mod.chatPanic(Env.chat.panic.get)).fuccess
  }

  def chatPanicPost = OAuthMod(_.Shadowban) { req => me =>
    val v = getBool("v", req)
    Env.chat.panic.set(v)
    Env.slack.api.chatPanic(me, v)
    fuccess(().some)
  }(ctx => me => _ => Redirect(routes.Mod.chatPanic).fuccess)

  def eventStream = OAuthSecure(_.Admin) { req => me =>
    noProxyBuffer(Ok.chunked(Env.mod.stream.enumerator)).fuccess
  }

  private def withSuspect[A](username: String)(f: Suspect => Fu[A])(implicit zero: Zero[A]): Fu[A] =
    Env.report.api getSuspect username flatMap {
      _ ?? f
    }

  private def OAuthMod[A](perm: Permission.Selector)(f: RequestHeader => UserModel => Fu[Option[A]])(
    secure: Context => UserModel => A => Fu[Result]
  ): Action[Unit] = SecureOrScoped(perm)(
    secure = ctx => me => f(ctx.req)(me) flatMap { _ ?? secure(ctx)(me) },
    scoped = req => me => f(req)(me) flatMap { res =>
      res.isDefined ?? fuccess(jsonOkResult)
    }
  )
  private def OAuthModBody[A](perm: Permission.Selector)(f: UserModel => Fu[Option[A]])(
    secure: BodyContext[_] => UserModel => A => Fu[Result]
  ): Action[AnyContent] = SecureOrScopedBody(perm)(
    secure = ctx => me => f(me) flatMap { _ ?? secure(ctx)(me) },
    scoped = _ => me => f(me) flatMap { res =>
      res.isDefined ?? fuccess(jsonOkResult)
    }
  )

  private def actionResult(username: String)(ctx: Context)(me: UserModel)(res: Any) =
    if (HTTPRequest isSynchronousHttp ctx.req) fuccess(Mod.redirect(username))
    else User.renderModZoneActions(username)(ctx)
}
