package controllers

import alleycats.Zero
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.Json
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.{ EmailAddress, HTTPRequest, IpAddress }
import lila.mod.ModUserSearch
import lila.report.{ Mod as AsMod, Suspect }
import lila.security.{ FingerHash, Granter, Permission }
import lila.user.{ User as UserModel }
import scala.annotation.nowarn

final class Mod(
    env: Env,
    reportC: => report.Report,
    userC: => User
)(using akka.stream.Materializer)
    extends LilaController(env):

  private def modApi    = env.mod.api
  private def assessApi = env.mod.assessApi

  private given Conversion[Me, AsMod] = me => AsMod(me)

  def alt(username: UserStr, v: Boolean) = OAuthModBody(_.CloseAccount) { me ?=>
    withSuspect(username): sus =>
      for
        _ <- modApi.setAlt(sus, v)
        _ <- (v && sus.user.enabled.yes) so env.api.accountClosure.close(sus.user)
        _ <- (!v && sus.user.enabled.no) so modApi.reopenAccount(sus.user.id)
      yield sus.some
  }(reportC.onModAction)

  def altMany = SecureBody(parse.tolerantText)(_.CloseAccount) { ctx ?=> me ?=>
    import akka.stream.scaladsl.*
    Source(ctx.body.body.split(' ').toList.flatMap(UserStr.read))
      .mapAsync(1): username =>
        withSuspect(username): sus =>
          modApi.setAlt(sus, true) >> (sus.user.enabled.yes so env.api.accountClosure.close(sus.user))
      .runWith(Sink.ignore)
      .void inject NoContent
  }

  def engine(username: UserStr, v: Boolean) =
    OAuthModBody(_.MarkEngine) { me ?=>
      withSuspect(username): sus =>
        modApi.setEngine(sus, v) inject sus.some
    }(reportC.onModAction)

  def publicChat = Secure(_.PublicChatView) { ctx ?=> _ ?=>
    env.mod.publicChat.all.flatMap: (tournamentsAndChats, swissesAndChats) =>
      Ok.page(html.mod.publicChat(tournamentsAndChats, swissesAndChats))
  }

  def publicChatTimeout = SecureOrScopedBody(_.ChatTimeout) { _ ?=> me ?=>
    lila.chat.ChatTimeout.form
      .bindFromRequest()
      .fold(
        form => BadRequest(form.errors mkString "\n"),
        data => env.chat.api.userChat.publicTimeout(data) inject NoContent
      )
  }

  def booster(username: UserStr, v: Boolean) = OAuthModBody(_.MarkBooster) { me ?=>
    withSuspect(username): prev =>
      modApi.setBoost(prev, v).map(some)
  }(reportC.onModAction)

  def troll(username: UserStr, v: Boolean) = OAuthModBody(_.Shadowban) { me ?=>
    withSuspect(username): prev =>
      for suspect <- modApi.setTroll(prev, v)
      yield suspect.some
  }(reportC.onModAction)

  def warn(username: UserStr, subject: String) = OAuthModBody(_.ModMessage) { me ?=>
    env.mod.presets.getPmPresets.named(subject).so { preset =>
      withSuspect(username): suspect =>
        for
          _ <- env.msg.api.systemPost(suspect.user.id, preset.text)
          _ <- env.mod.logApi.modMessage(suspect.user.id, preset.name)
          _ <- preset.isNameClose so env.irc.api.nameClosePreset(suspect.user.username)
        yield suspect.some
    }
  }(reportC.onModAction)

  def kid(username: UserStr) = OAuthMod(_.SetKidMode) { _ ?=> me ?=>
    modApi.setKid(me.id into ModId, username) map some
  }(actionResult(username))

  def deletePmsAndChats(username: UserStr) = OAuthMod(_.Shadowban) { _ ?=> _ ?=>
    withSuspect(username): sus =>
      env.mod.publicChat.deleteAll(sus) >>
        env.forum.delete.allByUser(sus.user) >>
        env.msg.api.deleteAllBy(sus.user) map some
  }(actionResult(username))

  def disableTwoFactor(username: UserStr) = OAuthMod(_.DisableTwoFactor) { _ ?=> me ?=>
    modApi.disableTwoFactor(me.id into ModId, username) map some
  }(actionResult(username))

  def closeAccount(username: UserStr) = OAuthMod(_.CloseAccount) { _ ?=> me ?=>
    env.user.repo byId username flatMapz { user =>
      env.api.accountClosure.close(user) map some
    }
  }(actionResult(username))

  def reopenAccount(username: UserStr) = OAuthMod(_.CloseAccount) { _ ?=> me ?=>
    modApi.reopenAccount(username) map some
  }(actionResult(username))

  def reportban(username: UserStr, v: Boolean) = OAuthMod(_.ReportBan) { _ ?=> me ?=>
    withSuspect(username): sus =>
      modApi.setReportban(sus, v) map some
  }(actionResult(username))

  def rankban(username: UserStr, v: Boolean) = OAuthMod(_.RemoveRanking) { _ ?=> me ?=>
    withSuspect(username): sus =>
      modApi.setRankban(sus, v) map some
  }(actionResult(username))

  def arenaBan(username: UserStr, v: Boolean) = OAuthMod(_.ArenaBan) { _ ?=> me ?=>
    withSuspect(username): sus =>
      modApi.setArenaBan(sus, v) map some
  }(actionResult(username))

  def prizeban(username: UserStr, v: Boolean) = OAuthMod(_.PrizeBan) { _ ?=> me ?=>
    withSuspect(username): sus =>
      modApi.setPrizeban(sus, v) map some
  }(actionResult(username))

  def impersonate(username: UserStr) = Auth { _ ?=> me ?=>
    if username == UserName("-") && env.mod.impersonate.isImpersonated(me) then
      env.mod.impersonate.stop(me)
      Redirect(routes.User.show(me.username))
    else if isGranted(_.Impersonate) || (isGranted(_.Admin) && username.id == lila.user.User.lichessId) then
      Found(env.user.repo byId username): user =>
        env.mod.impersonate.start(me, user)
        Redirect(routes.User.show(user.username))
    else notFound
  }

  def setTitle(username: UserStr) = SecureBody(_.SetTitle) { ctx ?=> me ?=>
    lila.user.UserForm.title
      .bindFromRequest()
      .fold(
        _ => redirect(username, mod = true),
        title =>
          modApi.setTitle(username, title) >>
            env.mailer.automaticEmail.onTitleSet(username) andDo
            env.user.lightUserApi.invalidate(username.id) inject
            redirect(username, mod = false)
      )
  }

  def setEmail(username: UserStr) = SecureBody(_.SetEmail) { ctx ?=> me ?=>
    Found(env.user.repo byId username): user =>
      env.security.forms
        .modEmail(user)
        .bindFromRequest()
        .fold(
          err => BadRequest(err.toString),
          email =>
            modApi.setEmail(user.id, email) inject
              redirect(user.username, mod = true)
        )
  }

  def inquiryToZulip = Secure(_.SendToZulip) { _ ?=> me ?=>
    env.report.api.inquiries ofModId me.id flatMap {
      case None => Redirect(report.routes.Report.list)
      case Some(report) =>
        Found(env.user.repo byId report.user): user =>
          import lila.report.Room
          import lila.irc.IrcApi.ModDomain
          env.irc.api.inquiry(
            user = user,
            domain = report.room match
              case Room.Cheat => ModDomain.Cheat
              case Room.Boost => ModDomain.Boost
              case Room.Comm  => ModDomain.Comm
              // spontaneous inquiry
              case _ if Granter(_.Admin)       => ModDomain.Admin
              case _ if Granter(_.CheatHunter) => ModDomain.Cheat // heuristic
              case _ if Granter(_.Shusher)     => ModDomain.Comm
              case _ if Granter(_.BoostHunter) => ModDomain.Boost
              case _                           => ModDomain.Admin
            ,
            room = if report.isSpontaneous then "Spontaneous inquiry" else report.room.name
          ) inject NoContent
    }
  }

  def createNameCloseVote(username: UserStr) = SendToZulip(username, env.irc.api.nameCloseVote)
  def askUsertableCheck(username: UserStr)   = SendToZulip(username, env.irc.api.usertableCheck)

  private def SendToZulip(username: UserStr, method: UserModel => Me ?=> Funit) =
    Secure(_.SendToZulip) { _ ?=> _ ?=>
      env.user.repo byId username orNotFound { method(_) inject NoContent }
    }

  def table = Secure(_.Admin) { ctx ?=> _ ?=>
    Ok.pageAsync:
      modApi.allMods.map(html.mod.table(_))
  }

  def log = Secure(_.GamifyView) { ctx ?=> me ?=>
    Ok.pageAsync:
      env.mod.logApi.recentBy(me).map(html.mod.log(_))
  }

  private def communications(username: UserStr, priv: Boolean) =
    Secure { perms =>
      if priv then perms.ViewPrivateComms else perms.Shadowban
    } { ctx ?=> me ?=>
      FoundPage(env.user.repo byId username): user =>
        given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply
        env.game.gameRepo
          .recentPovsByUserFromSecondary(user, 80)
          .mon(_.mod.comm.segment("recentPovs"))
          .flatMap: povs =>
            priv.so {
              env.chat.api.playerChat
                .optionsByOrderedIds(povs.map(_.gameId into ChatId))
                .mon(_.mod.comm.segment("playerChats"))
            } zip
              priv.so {
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
                if priv && !inquiry.so(_.isRecentCommOf(Suspect(user))) then
                  env.irc.api.commlog(user = user, inquiry.map(_.oldestAtom.by.userId))
                  if isGranted(_.MonitoredCommMod) then
                    env.irc.api.monitorMod(
                      "eyes",
                      s"spontaneously checked out @${user.username}'s private comms",
                      lila.irc.IrcApi.ModDomain.Comm
                    )
                env.appeal.api
                  .byUserIds(user.id :: logins.userLogins.otherUserIds)
                  .map: appeals =>
                    html.mod.communication(
                      me,
                      user,
                      povs
                        .zip(chats)
                        .collect:
                          case (p, Some(c)) if c.nonEmpty => p -> c
                        .take(15),
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

  def communicationPublic(username: UserStr)  = communications(username, priv = false)
  def communicationPrivate(username: UserStr) = communications(username, priv = true)

  protected[controllers] def redirect(username: UserStr, mod: Boolean = true) =
    Redirect(userUrl(username, mod))

  protected[controllers] def userUrl(username: UserStr, mod: Boolean = true) =
    s"${routes.User.show(username.value).url}${mod so "?mod"}"

  def refreshUserAssess(username: UserStr) = Secure(_.MarkEngine) { ctx ?=> me ?=>
    Found(env.user.repo byId username): user =>
      assessApi.refreshAssessOf(user) >>
        env.irwin.irwinApi.requests.fromMod(Suspect(user)) >>
        env.irwin.kaladinApi.modRequest(Suspect(user)) >>
        userC.renderModZoneActions(username)
  }

  def spontaneousInquiry(username: UserStr) = Secure(_.SeeReport) { ctx ?=> me ?=>
    Found(env.user.repo byId username): user =>
      (isGranted(_.Appeals) so env.appeal.api.exists(user)) flatMap { isAppeal =>
        isAppeal.so(env.report.api.inquiries.ongoingAppealOf(user.id)) flatMap {
          case Some(ongoing) if ongoing.mod != me.id =>
            env.user.lightUserApi
              .asyncFallback(ongoing.mod)
              .map: mod =>
                Redirect(appeal.routes.Appeal.show(user.username))
                  .flashFailure(s"Currently processed by ${mod.name}")
          case _ =>
            val f =
              if isAppeal then env.report.api.inquiries.appeal
              else env.report.api.inquiries.spontaneous
            f(Suspect(user)) inject {
              if isAppeal then Redirect(s"${appeal.routes.Appeal.show(user.username)}#appeal-actions")
              else redirect(user.username, mod = true)
            }
        }
      }
  }

  def gamify = Secure(_.GamifyView) { ctx ?=> _ ?=>
    for
      leaderboards <- env.mod.gamify.leaderboards
      history      <- env.mod.gamify.history(orCompute = true)
      page         <- renderPage(html.mod.gamify.index(leaderboards, history))
    yield Ok(page)
  }

  def gamifyPeriod(periodStr: String) = Secure(_.GamifyView) { ctx ?=> _ ?=>
    Found(lila.mod.Gamify.Period(periodStr)): period =>
      Ok.pageAsync:
        env.mod.gamify.leaderboards.map:
          html.mod.gamify.period(_, period)
  }

  def activity = activityOf("team", "month")

  def activityOf(who: String, period: String) = Secure(_.GamifyView) { ctx ?=> me ?=>
    Ok.pageAsync:
      env.mod.activity(who, period)(me.user).map(html.mod.activity(_))
  }

  def queues(period: String) = Secure(_.GamifyView) { ctx ?=> _ ?=>
    Ok.pageAsync:
      env.mod.queueStats(period).map(html.mod.queueStats(_))
  }

  def search = SecureBody(_.UserSearch) { ctx ?=> me ?=>
    ModUserSearch.form
      .bindFromRequest()
      .fold(err => BadRequest.page(html.mod.search(err, Nil)), searchTerm)
  }

  def notes(page: Int, q: String) = Secure(_.Admin) { _ ?=> _ ?=>
    Ok.pageAsync:
      env.user.noteApi.search(q.trim, page, withDox = true).map(html.mod.search.notes(q, _))
  }

  def gdprErase(username: UserStr) = Secure(_.GdprErase) { _ ?=> me ?=>
    val res = Redirect(routes.User.show(username.value))
    env.api.accountClosure
      .closeThenErase(username)
      .map:
        case Right(msg) => res flashSuccess msg
        case Left(err)  => res flashFailure err
  }

  protected[controllers] def searchTerm(query: String)(using Context, Me) =
    IpAddress.from(query) match
      case Some(ip) => Redirect(routes.Mod.singleIp(ip.value)).toFuccess
      case None =>
        for
          users <- env.mod.search(query)
          page  <- renderPage(html.mod.search(ModUserSearch.form.fill(query), users))
        yield Ok(page)

  def print(fh: String) = SecureBody(_.ViewPrintNoIP) { ctx ?=> me ?=>
    val hash = FingerHash(fh)
    for
      uids       <- env.security.api recentUserIdsByFingerHash hash
      users      <- env.user.repo usersFromSecondary uids.reverse
      withEmails <- env.user.api withEmails users
      uas        <- env.security.api.printUas(hash)
      page <- renderPage(html.mod.search.print(hash, withEmails, uas, env.security.printBan blocks hash))
    yield Ok(page)
  }

  def printBan(v: Boolean, fh: String) = Secure(_.PrintBan) { _ ?=> me ?=>
    val hash = FingerHash(fh)
    env.security.printBan.toggle(hash, v) inject Redirect(routes.Mod.print(fh))
  }

  def singleIp(ip: String) = SecureBody(_.ViewPrintNoIP) { ctx ?=> me ?=>
    given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply
    env.mod.ipRender.decrypt(ip) so { address =>
      for
        uids       <- env.security.api recentUserIdsByIp address
        users      <- env.user.repo usersFromSecondary uids.reverse
        withEmails <- env.user.api withEmails users
        uas        <- env.security.api.ipUas(address)
        data       <- env.security.ipTrust.data(address)
        blocked = env.security.firewall blocksIp address
        page <- renderPage(html.mod.search.ip(address, withEmails, uas, data, blocked))
      yield Ok(page)
    }
  }

  def singleIpBan(v: Boolean, ip: String) = Secure(_.IpBan) { ctx ?=> me ?=>
    val op =
      if v then env.security.firewall.blockIps
      else env.security.firewall.unblockIps
    val ipAddr = IpAddress from ip
    op(ipAddr.toList).inject:
      if HTTPRequest.isXhr(ctx.req) then jsonOkResult
      else Redirect(routes.Mod.singleIp(ip))
  }

  def chatUser(username: UserStr) = Secure(_.ChatTimeout) { _ ?=> _ ?=>
    JsonOptionOk:
      env.chat.api.userChat userModInfo username map2
        lila.chat.JsonView.userModInfo(using env.user.lightUserSync)
  }

  def permissions(username: UserStr) = Secure(_.ChangePermission) { _ ?=> _ ?=>
    FoundPage(env.user.repo byId username):
      html.mod.permissions(_)
  }

  def savePermissions(username: UserStr) = SecureBody(_.ChangePermission) { ctx ?=> me ?=>
    import lila.security.Permission
    Found(env.user.repo byId username): user =>
      Form(single("permissions" -> list(text.verifying(Permission.allByDbKey.contains))))
        .bindFromRequest()
        .fold(
          _ => BadRequest.page(html.mod.permissions(user)),
          permissions =>
            val newPermissions = Permission(permissions) diff Permission(user.roles)
            modApi.setPermissions(user.username, Permission(permissions)) >> {
              newPermissions(Permission.Coach) so env.mailer.automaticEmail.onBecomeCoach(user)
            } >> {
              Permission(permissions)
                .exists(_ is Permission.SeeReport) so env.plan.api.setLifetime(user)
            } inject Redirect(routes.Mod.permissions(user.username.value)).flashSuccess
        )
  }

  def emailConfirm = SecureBody(_.SetEmail) { ctx ?=> me ?=>
    get("q") match
      case None => Ok.page(html.mod.emailConfirm("", none, none))
      case Some(rawQuery) =>
        val query    = rawQuery.trim.split(' ').toList
        val email    = query.headOption.flatMap(EmailAddress.from)
        val username = query lift 1
        def tryWith(setEmail: EmailAddress, q: String): Fu[Option[Result]] =
          env.mod.search(q).map(_.filter(_.user.enabled.yes)).flatMap {
            case List(UserModel.WithEmails(user, _)) =>
              for
                _ <- (!user.everLoggedIn).so {
                  lila.mon.user.register.modConfirmEmail.increment()
                  modApi.setEmail(user.id, setEmail)
                }
                email <- env.user.repo.email(user.id)
                page  <- renderPage(html.mod.emailConfirm("", user.some, email))
              yield Ok(page).some
            case _ => fuccess(none)
          }
        email.so { em =>
          tryWith(em, em.value) orElse {
            username so { tryWith(em, _) }
          } recover lila.db.recoverDuplicateKey(_ => none)
        } getOrElse BadRequest.page(html.mod.emailConfirm(rawQuery, none, none))
  }

  def chatPanic = Secure(_.Shadowban) { ctx ?=> _ ?=>
    Ok.page(html.mod.chatPanic(env.chat.panic.get))
  }

  def chatPanicPost = OAuthMod(_.Shadowban) { ctx ?=> me ?=>
    val v = getBool("v")
    env.chat.panic.set(v)
    env.irc.api.chatPanic(me, v)
    fuccess(().some)
  }(_ => (_, _) ?=> Redirect(routes.Mod.chatPanic))

  def presets(group: String) = Secure(_.Presets) { ctx ?=> _ ?=>
    env.mod.presets
      .get(group)
      .fold(notFound): setting =>
        Ok.page(html.mod.presets(group, setting.form))
  }

  def presetsUpdate(group: String) = SecureBody(_.Presets) { ctx ?=> _ ?=>
    Found(env.mod.presets.get(group)): setting =>
      setting.form
        .bindFromRequest()
        .fold(
          err => BadRequest.page(html.mod.presets(group, err)),
          v => setting.setString(v.toString) inject Redirect(routes.Mod.presets(group)).flashSuccess
        )
  }

  def eventStream = SecuredScoped(_.Admin) { _ ?=> _ ?=>
    noProxyBuffer(Ok.chunked(env.mod.stream()))
  }

  def apiUserLog(username: UserStr) = SecuredScoped(_.ModLog) { _ ?=> me ?=>
    import lila.common.Json.given
    Found(env.user.repo byId username): user =>
      for
        logs      <- env.mod.logApi.userHistory(user.id)
        notes     <- env.socialInfo.fetchNotes(user)
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

  private def withSuspect[A: Zero](username: UserStr)(f: Suspect => Fu[A]): Fu[A] =
    env.report.api getSuspect username flatMapz f

  private def OAuthMod[A](perm: Permission.Selector)(f: Context ?=> Me ?=> Fu[Option[A]])(
      thenWhat: A => (Context, Me) ?=> Fu[Result]
  ): EssentialAction =
    SecureOrScoped(perm) { ctx ?=> me ?=>
      f.orNotFound: res =>
        if ctx.isOAuth then fuccess(jsonOkResult) else thenWhat(res)
    }
  private def OAuthModBody[A](perm: Permission.Selector)(f: Me ?=> Fu[Option[A]])(
      thenWhat: A => (BodyContext[?], Me) ?=> Fu[Result]
  ): EssentialAction =
    SecureOrScopedBody(perm) { ctx ?=> me ?=>
      f.orNotFound: res =>
        if ctx.isOAuth then fuccess(jsonOkResult) else thenWhat(res)
    }

  private def actionResult(username: UserStr)(@nowarn res: Any)(using ctx: Context, me: Me): Fu[Result] =
    if HTTPRequest.isSynchronousHttp(ctx.req)
    then redirect(username)
    else userC.renderModZoneActions(username)
