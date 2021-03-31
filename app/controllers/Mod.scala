package controllers

import ornicar.scalalib.Zero
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import scala.annotation.nowarn
import views._

import lila.api.{ BodyContext, Context }
import lila.app._
import lila.chat.Chat
import lila.common.{ EmailAddress, HTTPRequest, IpAddress }
import lila.mod.UserSearch
import lila.report.{ Suspect, Mod => AsMod }
import lila.security.{ FingerHash, Permission }
import lila.user.{ User => UserModel, Title, Holder }

final class Mod(
    env: Env,
    reportC: => Report,
    userC: => User
) extends LilaController(env) {

  private def modApi    = env.mod.api
  private def modLogApi = env.mod.logApi
  private def assessApi = env.mod.assessApi

  implicit private def asMod(holder: Holder) = AsMod(holder.user)

  def alt(username: String, v: Boolean) =
    OAuthModBody(_.CloseAccount) { me =>
      withSuspect(username) { sus =>
        for {
          inquiry <- env.report.api.inquiries ofModId me.id
          _       <- modApi.setAlt(me, sus, v)
          _       <- (v && sus.user.enabled) ?? env.closeAccount(sus.user.id, self = false)
        } yield (inquiry, sus).some
      }
    }(ctx =>
      me => { case (inquiry, suspect) =>
        reportC.onInquiryClose(inquiry, me, suspect.some)(ctx)
      }
    )

  def engine(username: String, v: Boolean) =
    OAuthModBody(_.MarkEngine) { me =>
      withSuspect(username) { sus =>
        for {
          inquiry <- env.report.api.inquiries ofModId me.id
          _       <- modApi.setEngine(me, sus, v)
        } yield (inquiry, sus).some
      }
    }(ctx =>
      me => { case (inquiry, suspect) =>
        reportC.onInquiryClose(inquiry, me, suspect.some)(ctx)
      }
    )

  def publicChat =
    Secure(_.ChatTimeout) { implicit ctx => _ =>
      env.mod.publicChat.all map { case (tournamentsAndChats, simulsAndChats) =>
        Ok(html.mod.publicChat(tournamentsAndChats, simulsAndChats))
      }
    }

  def publicChatTimeout =
    SecureBody(_.ChatTimeout) { implicit ctx => me =>
      FormResult(lila.chat.ChatTimeout.form) { data =>
        env.chat.api.userChat.publicTimeout(data, me)
      }(ctx.body)
    }

  def booster(username: String, v: Boolean) =
    OAuthModBody(_.MarkBooster) { me =>
      withSuspect(username) { prev =>
        for {
          inquiry <- env.report.api.inquiries ofModId me.id
          suspect <- modApi.setBoost(me, prev, v)
        } yield (inquiry, suspect).some
      }
    }(ctx =>
      me => { case (inquiry, suspect) =>
        reportC.onInquiryClose(inquiry, me, suspect.some)(ctx)
      }
    )

  def troll(username: String, v: Boolean) =
    OAuthModBody(_.Shadowban) { me =>
      withSuspect(username) { prev =>
        for {
          inquiry <- env.report.api.inquiries ofModId me.id
          suspect <- modApi.setTroll(me, prev, v)
        } yield (inquiry, suspect).some
      }
    }(ctx =>
      me => { case (inquiry, suspect) =>
        reportC.onInquiryClose(inquiry, me, suspect.some)(ctx)
      }
    )

  def warn(username: String, subject: String) =
    OAuthModBody(_.ModMessage) { me =>
      env.mod.presets.pmPresets.get().named(subject) ?? { preset =>
        withSuspect(username) { prev =>
          for {
            inquiry <- env.report.api.inquiries ofModId me.id
            suspect <- modApi.setTroll(me, prev, prev.user.marks.troll)
            _       <- env.msg.api.systemPost(suspect.user.id, preset.text)
            _       <- env.mod.logApi.modMessage(me.id, suspect.user.id, preset.name)
          } yield (inquiry, suspect).some
        }
      }
    }(ctx =>
      me => { case (inquiry, suspect) =>
        reportC.onInquiryClose(inquiry, me, suspect.some)(ctx)
      }
    )

  def kid(username: String) =
    OAuthMod(_.SetKidMode) { _ => me =>
      modApi.setKid(me.id, username) map some
    }(actionResult(username))

  def deletePmsAndChats(username: String) =
    OAuthMod(_.Shadowban) { _ => _ =>
      withSuspect(username) { sus =>
        env.mod.publicChat.deleteAll(sus) >>
          env.msg.api.deleteAllBy(sus.user) map some
      }
    }(actionResult(username))

  def disableTwoFactor(username: String) =
    Secure(_.DisableTwoFactor) { implicit ctx => me =>
      modApi.disableTwoFactor(me.id, username) >> userC.modZoneOrRedirect(me, username)
    }

  def closeAccount(username: String) =
    OAuthMod(_.CloseAccount) { _ => me =>
      env.user.repo named username flatMap {
        _ ?? { user =>
          modLogApi.closeAccount(me.id, user.id) >>
            env.closeAccount(user.id, self = false) map some
        }
      }
    }(actionResult(username))

  def reopenAccount(username: String) =
    OAuthMod(_.CloseAccount) { _ => me =>
      modApi.reopenAccount(me.id, username) map some
    }(actionResult(username))

  def reportban(username: String, v: Boolean) =
    OAuthMod(_.ReportBan) { _ => me =>
      withSuspect(username) { sus =>
        modApi.setReportban(me, sus, v) map some
      }
    }(actionResult(username))

  def rankban(username: String, v: Boolean) =
    OAuthMod(_.RemoveRanking) { _ => me =>
      withSuspect(username) { sus =>
        modApi.setRankban(me, sus, v) map some
      }
    }(actionResult(username))

  def impersonate(username: String) =
    Auth { implicit ctx => me =>
      if (username == "-" && env.mod.impersonate.isImpersonated(me)) fuccess {
        env.mod.impersonate.stop(me)
        Redirect(routes.User.show(me.username))
      }
      else if (isGranted(_.Impersonate) || (isGranted(_.Admin) && username.toLowerCase == "lichess"))
        OptionFuRedirect(env.user.repo named username) { user =>
          env.mod.impersonate.start(me, user)
          fuccess(routes.User.show(user.username))
        }
      else notFound
    }

  def setTitle(username: String) =
    SecureBody(_.SetTitle) { implicit ctx => me =>
      implicit def req = ctx.body
      lila.user.UserForm.title
        .bindFromRequest()
        .fold(
          _ => fuccess(redirect(username, mod = true)),
          title =>
            modApi.setTitle(me.id, username, title map Title.apply) >>
              env.security.automaticEmail.onTitleSet(username) >>-
              env.user.lightUserApi.invalidate(UserModel normalize username) inject
              redirect(username, mod = false)
        )
    }

  def setEmail(username: String) =
    SecureBody(_.SetEmail) { implicit ctx => me =>
      implicit def req = ctx.body
      OptionFuResult(env.user.repo named username) { user =>
        env.security.forms
          .modEmail(user)
          .bindFromRequest()
          .fold(
            err => BadRequest(err.toString).fuccess,
            rawEmail => {
              val email = env.security.emailAddressValidator
                .validate(EmailAddress(rawEmail)) err s"Invalid email $rawEmail"
              modApi.setEmail(me.id, user.id, email.acceptable) inject redirect(user.username, mod = true)
            }
          )
      }
    }

  def notifySlack(username: String) =
    OAuthMod(_.NotifySlack) { _ => me =>
      withSuspect(username) { sus =>
        env.irc.slack.userMod(user = sus.user, mod = me) map some
      }
    }(actionResult(username))

  def table =
    Secure(_.ModLog) { implicit ctx => _ =>
      modApi.allMods map { html.mod.table(_) }
    }

  private def communications(username: String, priv: Boolean) =
    Secure { perms =>
      if (priv) perms.ViewPrivateComms else perms.Shadowban
    } { implicit ctx => me =>
      OptionFuOk(env.user.repo named username) { user =>
        implicit val renderIp = env.mod.ipRender(me)
        env.game.gameRepo
          .recentPovsByUserFromSecondary(user, 80)
          .mon(_.mod.comm.segment("recentPovs"))
          .flatMap { povs =>
            priv.?? {
              env.chat.api.playerChat
                .optionsByOrderedIds(povs.map(_.gameId).map(Chat.Id.apply))
                .mon(_.mod.comm.segment("playerChats"))
            } zip
              priv.?? {
                env.msg.api
                  .recentByForMod(user, 30)
                  .mon(_.mod.comm.segment("pms"))
              } zip
              (env.shutup.api getPublicLines user.id)
                .mon(_.mod.comm.segment("publicChats")) zip
              env.user.noteApi
                .forMod(user.id)
                .mon(_.mod.comm.segment("notes")) zip
              env.mod.logApi
                .userHistory(user.id)
                .mon(_.mod.comm.segment("history")) zip
              env.report.api.inquiries
                .ofModId(me.id)
                .mon(_.mod.comm.segment("inquiries")) zip
              env.security.userLogins(user, 100).flatMap {
                userC.loginsTableData(user, _, 100)
              } map { case chats ~ convos ~ publicLines ~ notes ~ history ~ inquiry ~ logins =>
                if (priv) {
                  if (!inquiry.??(_.isRecentCommOf(Suspect(user)))) {
                    env.irc.slack.commlog(mod = me, user = user, inquiry.map(_.oldestAtom.by.value))
                    if (isGranted(_.MonitoredMod))
                      env.irc.slack.monitorMod(
                        me.id,
                        "eyes",
                        s"spontaneously checked out @${user.username}'s private comms",
                        lila.irc.SlackApi.MonitorType.Comm
                      )
                  }
                }
                html.mod.communication(
                  me,
                  user,
                  (povs zip chats) collect {
                    case (p, Some(c)) if c.nonEmpty => p -> c
                  } take 15,
                  convos,
                  publicLines,
                  notes.filter(_.from != "irwin"),
                  history,
                  logins,
                  priv
                )
              }
          }
      }
    }

  def communicationPublic(username: String)  = communications(username, priv = false)
  def communicationPrivate(username: String) = communications(username, priv = true)

  protected[controllers] def redirect(username: String, mod: Boolean = true) =
    Redirect(userUrl(username, mod))

  protected[controllers] def userUrl(username: String, mod: Boolean = true) =
    s"${routes.User.show(username).url}${mod ?? "?mod"}"

  def refreshUserAssess(username: String) =
    Secure(_.MarkEngine) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        assessApi.refreshAssessOf(user) >>
          env.irwin.api.requests.fromMod(Suspect(user), me) >>
          userC.renderModZoneActions(username)
      }
    }

  def spontaneousInquiry(username: String) =
    Secure(_.SeeReport) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        env.appeal.api.exists(user) flatMap { isAppeal =>
          val f =
            if (isAppeal) env.report.api.inquiries.appeal _
            else env.report.api.inquiries.spontaneous _
          f(me, Suspect(user)) inject {
            if (isAppeal) Redirect(routes.Appeal.show(user.username))
            else redirect(user.username, mod = true)
          }
        }
      }
    }

  def gamify =
    Secure(_.SeeReport) { implicit ctx => _ =>
      env.mod.gamify.leaderboards zip
        env.mod.gamify.history(orCompute = true) map { case (leaderboards, history) =>
          Ok(html.mod.gamify.index(leaderboards, history))
        }
    }
  def gamifyPeriod(periodStr: String) =
    Secure(_.SeeReport) { implicit ctx => _ =>
      lila.mod.Gamify.Period(periodStr).fold(notFound) { period =>
        env.mod.gamify.leaderboards map { leaderboards =>
          Ok(html.mod.gamify.period(leaderboards, period))
        }
      }
    }

  def search =
    SecureBody(_.UserSearch) { implicit ctx => _ =>
      implicit def req = ctx.body
      val f            = UserSearch.form
      f.bindFromRequest()
        .fold(
          err => BadRequest(html.mod.search(err, Nil)).fuccess,
          query => env.mod.search(query) map { html.mod.search(f.fill(query), _) }
        )
    }

  protected[controllers] def searchTerm(q: String)(implicit ctx: Context) = {
    val query = UserSearch exact q
    env.mod.search(query) map { users =>
      Ok(html.mod.search(UserSearch.form fill query, users))
    }
  }

  def print(fh: String) =
    SecureBody(_.ViewPrintNoIP) { implicit ctx => _ =>
      val hash = FingerHash(fh)
      for {
        uids       <- env.security.api recentUserIdsByFingerHash hash
        users      <- env.user.repo usersFromSecondary uids.reverse
        withEmails <- env.user.repo withEmailsU users
        uas        <- env.security.api.printUas(hash)
      } yield Ok(html.mod.search.print(hash, withEmails, uas, env.security.printBan blocks hash))
    }

  def printBan(v: Boolean, fh: String) =
    Secure(_.PrintBan) { _ => _ =>
      env.security.printBan.toggle(FingerHash(fh), v) inject
        Redirect(routes.Mod.print(fh))
    }

  def singleIp(ip: String) =
    SecureBody(_.ViewPrintNoIP) { implicit ctx => me =>
      implicit val renderIp = env.mod.ipRender(me)
      env.mod.ipRender.decrypt(ip) ?? { address =>
        for {
          uids       <- env.security.api recentUserIdsByIp address
          users      <- env.user.repo usersFromSecondary uids.reverse
          withEmails <- env.user.repo withEmailsU users
          uas        <- env.security.api.ipUas(address)
        } yield Ok(html.mod.search.ip(address, withEmails, uas, env.security.firewall blocksIp address))
      }
    }

  def singleIpBan(v: Boolean, ip: String) =
    Secure(_.IpBan) { ctx => _ =>
      val op =
        if (v) env.security.firewall.blockIps _
        else env.security.firewall.unblockIps _
      op(IpAddress from ip) inject {
        if (HTTPRequest isXhr ctx.req) jsonOkResult
        else Redirect(routes.Mod.singleIp(ip))
      }
    }

  def chatUser(username: String) =
    Secure(_.ChatTimeout) { _ => _ =>
      implicit val lightUser = env.user.lightUserSync
      JsonOptionOk {
        env.chat.api.userChat userModInfo username map2 lila.chat.JsonView.userModInfo
      }
    }

  def permissions(username: String) =
    Secure(_.ChangePermission) { implicit ctx => me =>
      OptionOk(env.user.repo named username) { user =>
        html.mod.permissions(user, me)
      }
    }

  def savePermissions(username: String) =
    SecureBody(_.ChangePermission) { implicit ctx => me =>
      implicit def req = ctx.body
      import lila.security.Permission
      OptionFuResult(env.user.repo named username) { user =>
        Form(
          single("permissions" -> list(text.verifying(Permission.allByDbKey.contains _)))
        ).bindFromRequest()
          .fold(
            _ => BadRequest(html.mod.permissions(user, me)).fuccess,
            permissions => {
              val newPermissions = Permission(permissions) diff Permission(user.roles)
              modApi.setPermissions(me, user.username, Permission(permissions)) >> {
                newPermissions(Permission.Coach) ?? env.security.automaticEmail.onBecomeCoach(user)
              } >> {
                Permission(permissions).exists(_ is Permission.SeeReport) ?? env.plan.api.setLifetime(user)
              } inject Redirect(routes.Mod.permissions(username)).flashSuccess
            }
          )
      }
    }

  def emailConfirm =
    SecureBody(_.SetEmail) { implicit ctx => me =>
      get("q") match {
        case None => Ok(html.mod.emailConfirm("", none, none)).fuccess
        case Some(rawQuery) =>
          val query = rawQuery.trim.split(' ').toList
          val email = query.headOption
            .map(EmailAddress.apply) flatMap env.security.emailAddressValidator.validate
          val username = query lift 1
          def tryWith(setEmail: EmailAddress, q: String): Fu[Option[Result]] =
            env.mod.search(UserSearch.exact(q)) flatMap {
              case List(UserModel.WithEmails(user, _)) =>
                (!user.everLoggedIn).?? {
                  lila.mon.user.register.modConfirmEmail.increment()
                  modApi.setEmail(me.id, user.id, setEmail)
                } >>
                  env.user.repo.email(user.id) map { email =>
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

  def chatPanic =
    Secure(_.Shadowban) { implicit ctx => _ =>
      Ok(html.mod.chatPanic(env.chat.panic.get)).fuccess
    }

  def chatPanicPost =
    OAuthMod(_.Shadowban) { req => me =>
      val v = getBool("v", req)
      env.chat.panic.set(v)
      env.irc.slack.chatPanic(me, v)
      fuccess(().some)
    }(_ => _ => _ => Redirect(routes.Mod.chatPanic).fuccess)

  def presets(group: String) =
    Secure(_.Presets) { implicit ctx => _ =>
      env.mod.presets.get(group).fold(notFound) { setting =>
        Ok(html.mod.presets(group, setting, setting.form)).fuccess
      }
    }

  def presetsUpdate(group: String) =
    SecureBody(_.Presets) { implicit ctx => _ =>
      implicit val req = ctx.body
      env.mod.presets.get(group).fold(notFound) { setting =>
        setting.form
          .bindFromRequest()
          .fold(
            err => BadRequest(html.mod.presets(group, setting, err)).fuccess,
            v => setting.setString(v.toString) inject Redirect(routes.Mod.presets(group)).flashSuccess
          )
      }
    }

  def eventStream =
    Scoped() { req => me =>
      IfGranted(_.Admin, req, me) {
        noProxyBuffer(Ok.chunked(env.mod.stream())).fuccess
      }
    }

  private def withSuspect[A](username: String)(f: Suspect => Fu[A])(implicit zero: Zero[A]): Fu[A] =
    env.report.api getSuspect username flatMap {
      _ ?? f
    }

  private def OAuthMod[A](perm: Permission.Selector)(f: RequestHeader => Holder => Fu[Option[A]])(
      secure: Context => Holder => A => Fu[Result]
  ): Action[Unit] =
    SecureOrScoped(perm)(
      secure = ctx => me => f(ctx.req)(me) flatMap { _ ?? secure(ctx)(me) },
      scoped = req =>
        me =>
          f(req)(me) flatMap { res =>
            res.isDefined ?? fuccess(jsonOkResult)
          }
    )
  private def OAuthModBody[A](perm: Permission.Selector)(f: Holder => Fu[Option[A]])(
      secure: BodyContext[_] => Holder => A => Fu[Result]
  ): Action[AnyContent] =
    SecureOrScopedBody(perm)(
      secure = ctx => me => f(me) flatMap { _ ?? secure(ctx)(me) },
      scoped = _ =>
        me =>
          f(me) flatMap { res =>
            res.isDefined ?? fuccess(jsonOkResult)
          }
    )

  private def actionResult(
      username: String
  )(ctx: Context)(@nowarn("cat=unused") user: Holder)(@nowarn("cat=unused") res: Any) =
    if (HTTPRequest isSynchronousHttp ctx.req) fuccess(redirect(username))
    else userC.renderModZoneActions(username)(ctx)
}
