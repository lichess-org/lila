package controllers

import alleycats.Zero
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.Json
import play.api.mvc.*
import views.*

import lila.api.{ BodyContext, Context }
import lila.app.{ given, * }
import lila.common.{ EmailAddress, HTTPRequest, IpAddress }
import lila.mod.UserSearch
import lila.report.{ Mod as AsMod, Suspect }
import lila.security.{ FingerHash, Granter, Permission }
import lila.user.{ Holder, User as UserModel }
import scala.annotation.nowarn

final class Mod(
    env: Env,
    reportC: => report.Report,
    userC: => User
) extends LilaController(env):

  private def modApi    = env.mod.api
  private def assessApi = env.mod.assessApi

  private given Conversion[Holder, AsMod] = holder => AsMod(holder.user)

  def alt(username: UserStr, v: Boolean) =
    OAuthModBody(_.CloseAccount) { me =>
      withSuspect(username) { sus =>
        for
          _ <- modApi.setAlt(me, sus, v)
          _ <- (v && sus.user.enabled.yes) ?? env.api.accountClosure.close(sus.user, me)
          _ <- (!v && sus.user.enabled.no) ?? modApi.reopenAccount(me.id into ModId, sus.user.id)
        yield sus.some
      }
    }(ctx =>
      me => { suspect =>
        reportC.onModAction(me, suspect)(using ctx)
      }
    )

  def engine(username: UserStr, v: Boolean) =
    OAuthModBody(_.MarkEngine) { me =>
      withSuspect(username) { sus =>
        for _ <- modApi.setEngine(me, sus, v)
        yield sus.some
      }
    }(ctx =>
      me => { suspect =>
        reportC.onModAction(me, suspect)(using ctx)
      }
    )

  def publicChat =
    Secure(_.PublicChatView) { implicit ctx => _ =>
      env.mod.publicChat.all map { case (tournamentsAndChats, swissesAndChats) =>
        Ok(html.mod.publicChat(tournamentsAndChats, swissesAndChats))
      }
    }

  def publicChatTimeout =
    def doTimeout(implicit req: Request[?], me: Holder) =
      FormResult(lila.chat.ChatTimeout.form) { data =>
        env.chat.api.userChat.publicTimeout(data, me)
      }
    SecureOrScopedBody(_.ChatTimeout)(
      secure = ctx => me => doTimeout(ctx.body, me),
      scoped = req => me => doTimeout(req, me)
    )

  def booster(username: UserStr, v: Boolean) =
    OAuthModBody(_.MarkBooster) { me =>
      withSuspect(username) { prev =>
        for suspect <- modApi.setBoost(me, prev, v)
        yield suspect.some
      }
    }(ctx =>
      me => { suspect =>
        reportC.onModAction(me, suspect)(using ctx)
      }
    )

  def troll(username: UserStr, v: Boolean) =
    OAuthModBody(_.Shadowban) { me =>
      withSuspect(username) { prev =>
        for suspect <- modApi.setTroll(me, prev, v)
        yield suspect.some
      }
    }(ctx =>
      me => { suspect =>
        reportC.onModAction(me, suspect)(using ctx)
      }
    )

  def warn(username: UserStr, subject: String) =
    OAuthModBody(_.ModMessage) { me =>
      env.mod.presets.getPmPresets(me.user).named(subject) ?? { preset =>
        withSuspect(username) { suspect =>
          for
            _ <- env.msg.api.systemPost(suspect.user.id, preset.text)
            _ <- env.mod.logApi.modMessage(me.id into ModId, suspect.user.id, preset.name)
            _ <- preset.isNameClose ?? env.irc.api.nameClosePreset(suspect.user.username)
          yield suspect.some
        }
      }
    }(ctx =>
      me => { suspect =>
        reportC.onModAction(me, suspect)(using ctx)
      }
    )

  def kid(username: UserStr) =
    OAuthMod(_.SetKidMode) { _ => me =>
      modApi.setKid(me.id into ModId, username) map some
    }(actionResult(username))

  def deletePmsAndChats(username: UserStr) =
    OAuthMod(_.Shadowban) { _ => _ =>
      withSuspect(username) { sus =>
        env.mod.publicChat.deleteAll(sus) >>
          env.forum.delete.allByUser(sus.user) >>
          env.msg.api.deleteAllBy(sus.user) map some
      }
    }(actionResult(username))

  def disableTwoFactor(username: UserStr) =
    OAuthMod(_.DisableTwoFactor) { _ => me =>
      modApi.disableTwoFactor(me.id into ModId, username) map some
    }(actionResult(username))

  def closeAccount(username: UserStr) =
    OAuthMod(_.CloseAccount) { _ => me =>
      env.user.repo byId username flatMapz { user =>
        env.api.accountClosure.close(user, me) map some
      }
    }(actionResult(username))

  def reopenAccount(username: UserStr) =
    OAuthMod(_.CloseAccount) { _ => me =>
      modApi.reopenAccount(me.id into ModId, username) map some
    }(actionResult(username))

  def reportban(username: UserStr, v: Boolean) =
    OAuthMod(_.ReportBan) { _ => me =>
      withSuspect(username) { sus =>
        modApi.setReportban(me, sus, v) map some
      }
    }(actionResult(username))

  def rankban(username: UserStr, v: Boolean) =
    OAuthMod(_.RemoveRanking) { _ => me =>
      withSuspect(username) { sus =>
        modApi.setRankban(me, sus, v) map some
      }
    }(actionResult(username))

  def impersonate(username: UserStr) =
    Auth { implicit ctx => me =>
      if (username == UserName("-") && env.mod.impersonate.isImpersonated(me)) fuccess {
        env.mod.impersonate.stop(me)
        Redirect(routes.User.show(me.username))
      }
      else if (isGranted(_.Impersonate) || (isGranted(_.Admin) && username.id == lila.user.User.lichessId))
        OptionFuRedirect(env.user.repo byId username) { user =>
          env.mod.impersonate.start(me, user)
          fuccess(routes.User.show(user.username))
        }
      else notFound
    }

  def setTitle(username: UserStr) =
    SecureBody(_.SetTitle) { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      lila.user.UserForm.title
        .bindFromRequest()
        .fold(
          _ => fuccess(redirect(username, mod = true)),
          title =>
            modApi.setTitle(me.id into ModId, username, title) >>
              env.mailer.automaticEmail.onTitleSet(username) >>-
              env.user.lightUserApi.invalidate(username.id) inject
              redirect(username, mod = false)
        )
    }

  def setEmail(username: UserStr) =
    SecureBody(_.SetEmail) { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      OptionFuResult(env.user.repo byId username) { user =>
        env.security.forms
          .modEmail(user)
          .bindFromRequest()
          .fold(
            err => BadRequest(err.toString).toFuccess,
            email =>
              modApi.setEmail(me.id into ModId, user.id, email) inject
                redirect(user.username, mod = true)
          )
      }
    }

  def inquiryToZulip =
    Secure(_.SendToZulip) { _ => me =>
      env.report.api.inquiries ofModId me.id flatMap {
        case None => Redirect(report.routes.Report.list).toFuccess
        case Some(report) =>
          env.user.repo byId report.user flatMapz { user =>
            import lila.report.Room
            import lila.irc.IrcApi.ModDomain
            env.irc.api.inquiry(
              user = user,
              mod = me,
              domain = report.room match
                case Room.Cheat => ModDomain.Cheat
                case Room.Boost => ModDomain.Boost
                case Room.Comm  => ModDomain.Comm
                // spontaneous inquiry
                case _ if Granter(_.Admin)(me.user)       => ModDomain.Admin
                case _ if Granter(_.CheatHunter)(me.user) => ModDomain.Cheat // heuristic
                case _ if Granter(_.Shusher)(me.user)     => ModDomain.Comm
                case _ if Granter(_.BoostHunter)(me.user) => ModDomain.Boost
                case _                                    => ModDomain.Admin
              ,
              room = if (report.isSpontaneous) "Spontaneous inquiry" else report.room.name
            ) inject NoContent
          }
      }
    }

  def createNameCloseVote(username: UserStr) = SendToZulip(username, env.irc.api.nameCloseVote)
  def askUsertableCheck(username: UserStr)   = SendToZulip(username, env.irc.api.usertableCheck)

  private def SendToZulip(username: UserStr, method: (UserModel, Holder) => Funit) =
    Secure(_.SendToZulip) { _ => me =>
      env.user.repo byId username flatMapz { method(_, me) inject NoContent }
    }

  def table =
    Secure(_.ModLog) { implicit ctx => _ =>
      modApi.allMods map { html.mod.table(_) }
    }

  def log =
    Secure(_.GamifyView) { implicit ctx => me =>
      env.mod.logApi.recentBy(me) map { html.mod.log(_) }
    }

  private def communications(username: UserStr, priv: Boolean) =
    Secure { perms =>
      if (priv) perms.ViewPrivateComms else perms.Shadowban
    } { implicit ctx => me =>
      OptionFuOk(env.user.repo byId username) { user =>
        given lila.mod.IpRender.RenderIp = env.mod.ipRender(me)
        env.game.gameRepo
          .recentPovsByUserFromSecondary(user, 80)
          .mon(_.mod.comm.segment("recentPovs"))
          .flatMap { povs =>
            priv.?? {
              env.chat.api.playerChat
                .optionsByOrderedIds(povs.map(_.gameId into ChatId))
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
                .byUserForMod(user.id)
                .mon(_.mod.comm.segment("notes")) zip
              env.mod.logApi
                .userHistory(user.id)
                .mon(_.mod.comm.segment("history")) zip
              env.report.api.inquiries
                .ofModId(me.id)
                .mon(_.mod.comm.segment("inquiries")) zip
              env.security.userLogins(user, 100).flatMap {
                userC.loginsTableData(user, _, 100)
              } flatMap { case ((((((chats, convos), publicLines), notes), history), inquiry), logins) =>
                if (priv)
                  if (!inquiry.??(_.isRecentCommOf(Suspect(user))))
                    env.irc.api.commlog(mod = me, user = user, inquiry.map(_.oldestAtom.by.userId))
                    if (isGranted(_.MonitoredMod))
                      env.irc.api.monitorMod(
                        me.id,
                        "eyes",
                        s"spontaneously checked out @${user.username}'s private comms",
                        lila.irc.IrcApi.ModDomain.Comm
                      )
                env.appeal.api.byUserIds(user.id :: logins.userLogins.otherUserIds) map { appeals =>
                  html.mod.communication(
                    me,
                    user,
                    (povs zip chats) collect {
                      case (p, Some(c)) if c.nonEmpty => p -> c
                    } take 15,
                    convos,
                    publicLines,
                    notes.filter(_.from != lila.user.User.irwinId),
                    history,
                    logins,
                    appeals,
                    priv
                  )
                }
              }
          }
      }
    }

  def communicationPublic(username: UserStr)  = communications(username, priv = false)
  def communicationPrivate(username: UserStr) = communications(username, priv = true)

  protected[controllers] def redirect(username: UserStr, mod: Boolean = true) =
    Redirect(userUrl(username, mod))

  protected[controllers] def userUrl(username: UserStr, mod: Boolean = true) =
    s"${routes.User.show(username.value).url}${mod ?? "?mod"}"

  def refreshUserAssess(username: UserStr) =
    Secure(_.MarkEngine) { implicit ctx => me =>
      OptionFuResult(env.user.repo byId username) { user =>
        assessApi.refreshAssessOf(user) >>
          env.irwin.irwinApi.requests.fromMod(Suspect(user), me) >>
          env.irwin.kaladinApi.modRequest(Suspect(user), me) >>
          userC.renderModZoneActions(username)
      }
    }

  def spontaneousInquiry(username: UserStr) =
    Secure(_.SeeReport) { implicit ctx => me =>
      OptionFuResult(env.user.repo byId username) { user =>
        (isGranted(_.Appeals) ?? env.appeal.api.exists(user)) flatMap { isAppeal =>
          isAppeal.??(env.report.api.inquiries.ongoingAppealOf(user.id)) flatMap {
            case Some(ongoing) if ongoing.mod != me.id =>
              env.user.lightUserApi.asyncFallback(ongoing.mod) map { mod =>
                Redirect(appeal.routes.Appeal.show(user.username))
                  .flashFailure(s"Currently processed by ${mod.name}")
              }
            case _ =>
              val f =
                if (isAppeal) env.report.api.inquiries.appeal
                else env.report.api.inquiries.spontaneous
              f(me, Suspect(user)) inject {
                if (isAppeal) Redirect(s"${appeal.routes.Appeal.show(user.username)}#appeal-actions")
                else redirect(user.username, mod = true)
              }
          }
        }
      }
    }

  def gamify =
    Secure(_.GamifyView) { implicit ctx => _ =>
      env.mod.gamify.leaderboards zip
        env.mod.gamify.history(orCompute = true) map { case (leaderboards, history) =>
          Ok(html.mod.gamify.index(leaderboards, history))
        }
    }
  def gamifyPeriod(periodStr: String) =
    Secure(_.GamifyView) { implicit ctx => _ =>
      lila.mod.Gamify.Period(periodStr).fold(notFound) { period =>
        env.mod.gamify.leaderboards map { leaderboards =>
          Ok(html.mod.gamify.period(leaderboards, period))
        }
      }
    }

  def activity = activityOf("team", "month")

  def activityOf(who: String, period: String) =
    Secure(_.GamifyView) { implicit ctx => me =>
      env.mod.activity(who, period)(me.user) map { activity =>
        Ok(html.mod.activity(activity))
      }
    }

  def queues(period: String) =
    Secure(_.GamifyView) { implicit ctx => _ =>
      env.mod.queueStats(period) map { stats =>
        Ok(html.mod.queueStats(stats))
      }
    }

  def search =
    SecureBody(_.UserSearch) { implicit ctx => me =>
      given Request[?] = ctx.body
      UserSearch.form
        .bindFromRequest()
        .fold(
          err => BadRequest(html.mod.search(me, err, Nil)).toFuccess,
          query => env.mod.search(query) map { html.mod.search(me, UserSearch.form.fill(query), _) }
        )
    }

  def gdprErase(username: UserStr) =
    Secure(_.GdprErase) { _ => me =>
      val res = Redirect(routes.User.show(username.value))
      env.api.accountClosure.closeThenErase(username, me) map {
        case Right(msg) => res flashSuccess msg
        case Left(err)  => res flashFailure err
      }
    }

  protected[controllers] def searchTerm(me: Holder, q: String)(implicit ctx: Context) =
    env.mod.search(q) map { users =>
      Ok(html.mod.search(me, UserSearch.form fill q, users))
    }

  def print(fh: String) =
    SecureBody(_.ViewPrintNoIP) { implicit ctx => me =>
      val hash = FingerHash(fh)
      for
        uids       <- env.security.api recentUserIdsByFingerHash hash
        users      <- env.user.repo usersFromSecondary uids.reverse
        withEmails <- env.user.repo withEmails users
        uas        <- env.security.api.printUas(hash)
      yield Ok(html.mod.search.print(me, hash, withEmails, uas, env.security.printBan blocks hash))
    }

  def printBan(v: Boolean, fh: String) =
    Secure(_.PrintBan) { _ => me =>
      val hash = FingerHash(fh)
      env.security.printBan.toggle(hash, v) >>
        env.security.api.recentUserIdsByFingerHash(hash) flatMap { userIds =>
          env.irc.api.printBan(me, fh, v, userIds)
        } inject Redirect(routes.Mod.print(fh))
    }

  def singleIp(ip: String) =
    SecureBody(_.ViewPrintNoIP) { implicit ctx => me =>
      given lila.mod.IpRender.RenderIp = env.mod.ipRender(me)
      env.mod.ipRender.decrypt(ip) ?? { address =>
        for
          uids       <- env.security.api recentUserIdsByIp address
          users      <- env.user.repo usersFromSecondary uids.reverse
          withEmails <- env.user.repo withEmails users
          uas        <- env.security.api.ipUas(address)
        yield Ok(html.mod.search.ip(me, address, withEmails, uas, env.security.firewall blocksIp address))
      }
    }

  def singleIpBan(v: Boolean, ip: String) =
    Secure(_.IpBan) { ctx => me =>
      val op =
        if (v) env.security.firewall.blockIps
        else env.security.firewall.unblockIps
      val ipAddr = IpAddress from ip
      op(ipAddr) >> (ipAddr ?? {
        env.security.api.recentUserIdsByIp(_) flatMap { userIds =>
          env.irc.api.ipBan(me, ip, v, userIds)
        }
      }) inject {
        if (HTTPRequest isXhr ctx.req) jsonOkResult
        else Redirect(routes.Mod.singleIp(ip))
      }
    }

  def chatUser(username: UserStr) =
    Secure(_.ChatTimeout) { _ => _ =>
      JsonOptionOk {
        env.chat.api.userChat userModInfo username map2
          lila.chat.JsonView.userModInfo(using env.user.lightUserSync)
      }
    }

  def permissions(username: UserStr) =
    Secure(_.ChangePermission) { implicit ctx => me =>
      OptionOk(env.user.repo byId username) { user =>
        html.mod.permissions(user, me)
      }
    }

  def savePermissions(username: UserStr) =
    SecureBody(_.ChangePermission) { implicit ctx => me =>
      given Request[?] = ctx.body
      import lila.security.Permission
      OptionFuResult(env.user.repo byId username) { user =>
        Form(
          single("permissions" -> list(text.verifying(Permission.allByDbKey.contains)))
        ).bindFromRequest()
          .fold(
            _ => BadRequest(html.mod.permissions(user, me)).toFuccess,
            permissions => {
              val newPermissions = Permission(permissions) diff Permission(user.roles)
              modApi.setPermissions(me, user.username, Permission(permissions)) >> {
                newPermissions(Permission.Coach) ?? env.mailer.automaticEmail.onBecomeCoach(user)
              } >> {
                Permission(permissions)
                  .exists(_ is Permission.SeeReport) ?? env.plan.api.setLifetime(user)
              } inject Redirect(routes.Mod.permissions(user.username.value)).flashSuccess
            }
          )
      }
    }

  def emailConfirm =
    SecureBody(_.SetEmail) { implicit ctx => me =>
      get("q") match
        case None => Ok(html.mod.emailConfirm("", none, none)).toFuccess
        case Some(rawQuery) =>
          val query    = rawQuery.trim.split(' ').toList
          val email    = query.headOption.flatMap(EmailAddress.from)
          val username = query lift 1
          def tryWith(setEmail: EmailAddress, q: String): Fu[Option[Result]] =
            env.mod.search(q) flatMap {
              case List(UserModel.WithEmails(user, _)) =>
                (!user.everLoggedIn).?? {
                  lila.mon.user.register.modConfirmEmail.increment()
                  modApi.setEmail(me.id into ModId, user.id, setEmail)
                } >>
                  env.user.repo.email(user.id) map { email =>
                    Ok(html.mod.emailConfirm("", user.some, email)).some
                  }
              case _ => fuccess(none)
            }
          email.?? { em =>
            tryWith(em, em.value) orElse {
              username ?? { tryWith(em, _) }
            } recover lila.db.recoverDuplicateKey(_ => none)
          } getOrElse BadRequest(html.mod.emailConfirm(rawQuery, none, none)).toFuccess
    }

  def chatPanic =
    Secure(_.Shadowban) { implicit ctx => _ =>
      Ok(html.mod.chatPanic(env.chat.panic.get)).toFuccess
    }

  def chatPanicPost =
    OAuthMod(_.Shadowban) { req => me =>
      val v = getBool("v", req)
      env.chat.panic.set(v)
      env.irc.api.chatPanic(me, v)
      fuccess(().some)
    }(_ => _ => _ => Redirect(routes.Mod.chatPanic).toFuccess)

  def presets(group: String) =
    Secure(_.Presets) { implicit ctx => _ =>
      env.mod.presets.get(group).fold(notFound) { setting =>
        Ok(html.mod.presets(group, setting.form)).toFuccess
      }
    }

  def presetsUpdate(group: String) =
    SecureBody(_.Presets) { implicit ctx => _ =>
      given Request[?] = ctx.body
      env.mod.presets.get(group).fold(notFound) { setting =>
        setting.form
          .bindFromRequest()
          .fold(
            err => BadRequest(html.mod.presets(group, err)).toFuccess,
            v => setting.setString(v.toString) inject Redirect(routes.Mod.presets(group)).flashSuccess
          )
      }
    }

  def eventStream =
    Scoped() { req => me =>
      IfGranted(_.Admin, req, me) {
        noProxyBuffer(Ok.chunked(env.mod.stream())).toFuccess
      }
    }

  def apiUserLog(username: UserStr) =
    SecureScoped(_.ModLog) { _ => me =>
      import lila.common.Json.given
      env.user.repo byId username flatMapz { user =>
        for
          logs      <- env.mod.logApi.userHistory(user.id)
          notes     <- env.socialInfo.fetchNotes(user, me.user)
          notesJson <- lila.user.JsonView.notes(notes)(using env.user.lightUserApi)
        yield JsonOk(
          Json.obj(
            "logs" -> Json.arr(logs.map { log =>
              Json
                .obj("mod" -> log.mod, "action" -> log.action, "date" -> log.date)
                .add("details", log.details)
            }),
            "notes" -> notesJson
          )
        )
      }
    }

  private def withSuspect[A: Zero](username: UserStr)(f: Suspect => Fu[A]): Fu[A] =
    env.report.api getSuspect username flatMapz f

  private def OAuthMod[A](perm: Permission.Selector)(f: RequestHeader => Holder => Fu[Option[A]])(
      secure: Context => Holder => A => Fu[Result]
  ): Action[Unit] =
    SecureOrScoped(perm)(
      secure = ctx => me => f(ctx.req)(me) flatMapz secure(ctx)(me),
      scoped = req =>
        me =>
          f(req)(me) flatMap { res =>
            res.isDefined ?? fuccess(jsonOkResult)
          }
    )
  private def OAuthModBody[A](perm: Permission.Selector)(f: Holder => Fu[Option[A]])(
      secure: BodyContext[?] => Holder => A => Fu[Result]
  ): Action[AnyContent] =
    SecureOrScopedBody(perm)(
      secure = ctx => me => f(me) flatMapz secure(ctx)(me),
      scoped = _ =>
        me =>
          f(me) flatMap { res =>
            res.isDefined ?? fuccess(jsonOkResult)
          }
    )

  private def actionResult(
      username: UserStr
  )(ctx: Context)(@nowarn user: Holder)(@nowarn res: Any) =
    if HTTPRequest.isSynchronousHttp(ctx.req)
    then fuccess(redirect(username))
    else userC.renderModZoneActions(username)(ctx)
