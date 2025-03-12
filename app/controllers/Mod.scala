package controllers

import alleycats.Zero
import play.api.libs.json.Json
import play.api.mvc.*

import scala.annotation.nowarn

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.net.IpAddress
import lila.core.perm.Permission
import lila.core.security.FingerHash
import lila.core.userId.ModId
import lila.mod.{ Modlog, ModUserSearch }
import lila.report.{ Mod as AsMod, Suspect }

final class Mod(
    env: Env,
    reportC: => report.Report,
    userC: => User
)(using akka.stream.Materializer)
    extends LilaController(env):

  import env.mod.{ api, assessApi }

  private given Conversion[Me, AsMod] = me => AsMod(me)

  def alt(username: UserStr, v: Boolean) = OAuthModBody(_.CloseAccount) { me ?=>
    withSuspect(username): sus =>
      for
        _ <- api.setAlt(sus, v)
        _ <- (v && sus.user.enabled.yes).so(env.api.accountTermination.disable(sus.user, forever = false))
        _ <- (!v && sus.user.enabled.no).so(api.reopenAccount(sus.user.id))
      yield sus.some
  }(reportC.onModAction)

  def altMany = SecureBody(parse.tolerantText)(_.CloseAccount) { ctx ?=> me ?=>
    import akka.stream.scaladsl.*
    Source(ctx.body.body.split(' ').toList.flatMap(UserStr.read))
      .mapAsync(1): username =>
        withSuspect(username): sus =>
          api.setAlt(sus, true) >> (sus.user.enabled.yes.so(
            env.api.accountTermination.disable(sus.user, forever = false)
          ))
      .runWith(Sink.ignore)
      .void
      .inject(NoContent)
  }

  def engine(username: UserStr, v: Boolean) =
    OAuthModBody(_.MarkEngine) { me ?=>
      withSuspect(username): sus =>
        api.setEngine(sus, v).inject(sus.some)
    }(reportC.onModAction)

  def publicChat = Secure(_.PublicChatView) { ctx ?=> _ ?=>
    env.mod.publicChat.all.flatMap: (tournamentsAndChats, swissesAndChats) =>
      Ok.page(views.mod.publicChat(tournamentsAndChats, swissesAndChats))
  }

  def publicChatTimeout = SecureOrScopedBody(_.ChatTimeout) { _ ?=> me ?=>
    bindForm(lila.chat.ChatTimeout.form)(
      form => BadRequest(form.errors.mkString("\n")),
      data => env.chat.api.userChat.publicTimeout(data).inject(NoContent)
    )
  }

  def booster(username: UserStr, v: Boolean) = OAuthModBody(_.MarkBooster) { me ?=>
    withSuspect(username): prev =>
      api.setBoost(prev, v).dmap(some)
  }(reportC.onModAction)

  def troll(username: UserStr, v: Boolean) = OAuthModBody(_.Shadowban) { me ?=>
    withSuspect(username): prev =>
      for suspect <- api.setTroll(prev, v)
      yield suspect.some
  }(reportC.onModAction)

  def isolate(username: UserStr, v: Boolean) = OAuthModBody(_.Shadowban) { me ?=>
    withSuspect(username): prev =>
      for
        suspect <- api.setIsolate(prev, v)
        _       <- env.relation.api.removeAllFollowers(suspect.user.id)
      yield suspect.some
  }(reportC.onModAction)

  def warn(username: UserStr, subject: String) = OAuthModBody(_.ModMessage) { me ?=>
    env.mod.presets.getPmPresets.named(subject).so { preset =>
      withSuspect(username): suspect =>
        for
          _ <- env.msg.api.systemPost(suspect.user.id, preset.text)
          _ <- env.mod.logApi.modMessage(suspect.user.id, preset.name)
          _ <- preset.isNameClose.so(env.irc.api.nameClosePreset(suspect.user.username))
        yield suspect.some
    }
  }(reportC.onModAction)

  def kid(username: UserStr) = OAuthMod(_.SetKidMode) { _ ?=> me ?=>
    api.setKid(me.id.into(ModId), username).dmap(some)
  }(actionResult(username))

  def deletePmsAndChats(username: UserStr) = OAuthMod(_.Shadowban) { _ ?=> _ ?=>
    withSuspect(username): sus =>
      for
        _ <- env.mod.publicChat.deleteAll(sus)
        _ <- env.forum.delete.allByUser(sus.user)
        _ <- env.msg.api.deleteAllBy(sus.user)
        _ <- env.mod.logApi.deleteComms(sus)
      yield ().some
  }(actionResult(username))

  def disableTwoFactor(username: UserStr) = OAuthMod(_.DisableTwoFactor) { _ ?=> me ?=>
    api.disableTwoFactor(me.id.into(ModId), username).dmap(some)
  }(actionResult(username))

  def closeAccount(username: UserStr) = OAuthMod(_.CloseAccount) { _ ?=> me ?=>
    meOrFetch(username).flatMapz: user =>
      env.api.accountTermination.disable(user, forever = false).dmap(some)
  }(actionResult(username))

  def reopenAccount(username: UserStr) = OAuthMod(_.CloseAccount) { _ ?=> me ?=>
    api.reopenAccount(username).dmap(some)
  }(actionResult(username))

  def reportban(username: UserStr, v: Boolean) = OAuthMod(_.ReportBan) { _ ?=> me ?=>
    withSuspect(username): sus =>
      api.setReportban(sus, v).dmap(some)
  }(actionResult(username))

  def rankban(username: UserStr, v: Boolean) = OAuthMod(_.RemoveRanking) { _ ?=> me ?=>
    withSuspect(username): sus =>
      api.setRankban(sus, v).dmap(some)
  }(actionResult(username))

  def arenaBan(username: UserStr, v: Boolean) = OAuthMod(_.ArenaBan) { _ ?=> me ?=>
    withSuspect(username): sus =>
      api.setArenaBan(sus, v).dmap(some)
  }(actionResult(username))

  def prizeban(username: UserStr, v: Boolean) = OAuthMod(_.PrizeBan) { _ ?=> me ?=>
    withSuspect(username): sus =>
      api.setPrizeban(sus, v).dmap(some)
  }(actionResult(username))

  def impersonate(username: String) = Auth { _ ?=> me ?=>
    if env.mod.impersonate.isImpersonated(me) then
      env.mod.impersonate.stop(me)
      Redirect(routes.User.show(me.username))
    else
      UserStr
        .read(username)
        .so: username =>
          if isGranted(_.Impersonate) || (isGranted(_.Admin) && username.is(UserId.lichess)) then
            Found(env.user.repo.byId(username)): user =>
              env.mod.impersonate.start(me, user)
              Redirect(routes.User.show(user.username))
          else notFound
  }

  def setTitle(username: UserStr) = SecureBody(_.SetTitle) { ctx ?=> me ?=>
    bindForm(lila.user.UserForm.title)(
      _ => redirect(username, mod = true),
      title =>
        doSetTitle(username.id, title).inject:
          redirect(username, mod = false)
    )
  }

  protected[controllers] def doSetTitle(userId: UserId, title: Option[chess.PlayerTitle])(using Me) = for
    _ <- api.setTitle(userId, title)
    _ <- title.so(env.mailer.automaticEmail.onTitleSet(userId, _))
  yield ()

  def setEmail(username: UserStr) = SecureBody(_.SetEmail) { ctx ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      bindForm(env.security.forms.modEmail(user))(
        err => BadRequest(err.toString),
        email => api.setEmail(user.id, email).inject(redirect(user.username, mod = true))
      )
  }

  def inquiryToZulip = Secure(_.SendToZulip) { _ ?=> me ?=>
    env.report.api.inquiries
      .ofModId(me.id)
      .flatMap:
        case None => Redirect(routes.Report.list)
        case Some(report) =>
          Found(env.user.repo.byId(report.user)): user =>
            import lila.report.Room
            import lila.core.irc.ModDomain
            env.irc.api
              .inquiry(
                user = user.light,
                domain = report.room match
                  case Room.Cheat => ModDomain.Cheat
                  case Room.Boost => ModDomain.Boost
                  case Room.Comm  => ModDomain.Comm
                  // spontaneous inquiry
                  case _ if isGranted(_.Admin)       => ModDomain.Admin
                  case _ if isGranted(_.CheatHunter) => ModDomain.Cheat // heuristic
                  case _ if isGranted(_.Shusher)     => ModDomain.Comm
                  case _ if isGranted(_.BoostHunter) => ModDomain.Boost
                  case _                             => ModDomain.Admin
                ,
                room = if report.isSpontaneous then "Spontaneous inquiry" else report.room.name
              )
              .inject(NoContent)
  }

  def createNameCloseVote(username: UserStr) = Secure(_.SendToZulip) { _ ?=> me ?=>
    env.report.api.inquiries.myUsernameReportText.flatMap: txt =>
      env.user.repo.byId(username).orNotFound { user =>
        val details = s"created on: ${user.createdAt.date}, ${user.count.game} games"
        env.irc.api
          .nameCloseVote(user.light, details, txt)
          .inject(NoContent)
      }
  }

  def askUsertableCheck(username: UserStr) = Secure(_.SendToZulip) { _ ?=> _ ?=>
    env.user.lightUser(username.id).orNotFound { env.irc.api.usertableCheck(_).inject(NoContent) }
  }

  def table = Secure(_.Admin) { ctx ?=> _ ?=>
    Ok.async:
      api.allMods.map(views.mod.userTable.mods(_))
  }

  def log = Secure(_.GamifyView) { ctx ?=> me ?=>
    Ok.async:
      for
        log     <- env.mod.logApi.recentBy(me)
        appeals <- env.appeal.api.myLog(log.lastOption.map(_.date).|(nowInstant.minusMonths(1)))
        appealsLog = appeals.map: (user, msg) =>
          Modlog(user.some, "appeal", msg.text.some).copy(date = msg.at)
        sorted = (log ::: appealsLog).sortBy(_.date).reverse
      yield views.mod.ui.myLogs(sorted)
  }

  private def communications(username: UserStr, priv: Boolean) =
    Secure(perms => if priv then perms.ViewPrivateComms else perms.Shadowban) { ctx ?=> me ?=>
      FoundPage(env.user.repo.byId(username)): user =>
        given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply
        env.game.gameRepo
          .recentPovsByUserFromSecondary(user, 80)
          .mon(_.mod.comm.segment("recentPovs"))
          .flatMap: povs =>
            (
              env.api.modTimeline.load(user, withPlayBans = false).mon(_.mod.comm.segment("modTimeline")),
              priv.so:
                env.chat.api.playerChat
                  .optionsByOrderedIds(povs.map(_.gameId.into(ChatId)))
                  .mon(_.mod.comm.segment("playerChats"))
              ,
              priv.so:
                env.msg.api
                  .recentByForMod(user, 30)
                  .mon(_.mod.comm.segment("pms"))
              ,
              env.shutup.api
                .getPublicLines(user.id)
                .mon(_.mod.comm.segment("publicChats")),
              env.report.api.inquiries
                .ofModId(me.id)
                .mon(_.mod.comm.segment("inquiries")),
              env.security.userLogins(user, 100).flatMap {
                userC.loginsTableData(user, _, 100)
              }
            ).flatMapN { (timeline, chats, convos, publicLines, inquiry, logins) =>
              if priv && !inquiry.so(_.isRecentCommOf(Suspect(user))) then
                env.irc.api.commlog(user = user.light, inquiry.map(_.oldestAtom.by.userId))
                if isGranted(_.MonitoredCommMod) then
                  env.irc.api.monitorMod(
                    "eyes",
                    s"spontaneously checked out @${user.username}'s private comms",
                    lila.core.irc.ModDomain.Comm
                  )
              env.appeal.api
                .byUserIds(user.id :: logins.userLogins.otherUserIds)
                .map: appeals =>
                  views.mod.communication(
                    me,
                    timeline,
                    povs
                      .zip(chats)
                      .collect:
                        case (p, Some(c)) if c.nonEmpty => p -> c
                      .take(15),
                    convos,
                    publicLines,
                    logins,
                    appeals,
                    priv
                  )
            }
    }

  def communicationPublic(username: UserStr)  = communications(username, priv = false)
  def communicationPrivate(username: UserStr) = communications(username, priv = true)

  def fullCommsExport(username: UserStr) =
    SecureBody(_.FullCommsExport) { ctx ?=> me ?=>
      Found(env.user.repo.byId(username)): user =>
        val source = env.msg.api
          .modFullCommsExport(user.id)
          .map: (tid, msgs) =>
            s"=== 0 === thread: ${tid}\n${msgs.map(m => s"${m.date} ${m.user}: ${m.text}\n--- 0 ---\n").toList.mkString("\n")}"
        env.mod.logApi.fullCommExport(Suspect(user))
        env.irc.api.fullCommExport(user.light)
        Ok.chunked(source).pipe(asAttachmentStream(s"full-comms-export-of-${user.id}.txt"))
    }

  protected[controllers] def redirect(username: UserStr, mod: Boolean = true) =
    Redirect(userUrl(username, mod))

  protected[controllers] def userUrl(username: UserStr, mod: Boolean = true) =
    s"${routes.User.show(username).url}${mod.so("?mod")}"

  def refreshUserAssess(username: UserStr) = Secure(_.MarkEngine) { ctx ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      assessApi.refreshAssessOf(user) >>
        env.irwin.irwinApi.requests.fromMod(Suspect(user)) >>
        env.irwin.kaladinApi.modRequest(Suspect(user)) >>
        userC.renderModZoneActions(username)
  }

  def spontaneousInquiry(username: UserStr) = Secure(_.SeeReport) { ctx ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      (getBool("appeal") && isGranted(_.Appeals)).so(env.appeal.api.exists(user)).flatMap { isAppeal =>
        isAppeal
          .so(env.report.api.inquiries.ongoingAppealOf(user.id))
          .flatMap:
            case Some(ongoing) if ongoing.mod != me.id =>
              env.user.lightUserApi
                .asyncFallback(ongoing.mod)
                .map: mod =>
                  Redirect(routes.Appeal.show(user.username))
                    .flashFailure(s"Currently processed by ${mod.name}")
            case _ =>
              val f =
                if isAppeal then env.report.api.inquiries.appeal
                else env.report.api.inquiries.spontaneous
              f(Suspect(user)).inject:
                if isAppeal then Redirect(s"${routes.Appeal.show(user.username)}#appeal-actions")
                else redirect(user.username, mod = true)
      }
  }

  def gamify = Secure(_.GamifyView) { ctx ?=> _ ?=>
    for
      leaderboards <- env.mod.gamify.leaderboards
      history      <- env.mod.gamify.history(orCompute = true)
      page         <- renderPage(views.mod.gamify.index(leaderboards, history))
    yield Ok(page)
  }

  def gamifyPeriod(periodStr: String) = Secure(_.GamifyView) { ctx ?=> _ ?=>
    Found(lila.mod.Gamify.Period(periodStr)): period =>
      Ok.async:
        env.mod.gamify.leaderboards.map:
          views.mod.gamify.period(_, period)
  }

  def activity = activityOf("team", "month")

  def activityOf(who: String, period: String) = Secure(_.GamifyView) { ctx ?=> me ?=>
    Ok.async:
      env.mod.activity(who, period)(me.user).map(views.mod.ui.activity(_))
  }

  def queues(period: String) = Secure(_.GamifyView) { ctx ?=> _ ?=>
    Ok.async:
      env.mod.queueStats(period).map(views.mod.ui.queueStats(_))
  }

  def search = SecureBody(_.UserSearch) { ctx ?=> me ?=>
    bindForm(ModUserSearch.form)(err => BadRequest.page(views.mod.search(err, none)), searchTerm)
  }

  def notes(page: Int, q: String) = Secure(_.Admin) { _ ?=> _ ?=>
    Ok.async:
      env.user.noteApi.search(q.trim, page, withDox = true).map(views.mod.search.notes(q, _))
  }

  def gdprErase(username: UserStr) = Secure(_.GdprErase) { _ ?=> _ ?=>
    Found(env.user.repo.byId(username)): user =>
      for _ <- env.api.accountTermination.scheduleDelete(user)
      yield Redirect(routes.User.show(username)).flashSuccess("Erasure scheduled")
  }

  protected[controllers] def searchTerm(query: String)(using Context, Me) =
    IpAddress.from(query) match
      case Some(ip) => Redirect(routes.Mod.singleIp(ip.value)).toFuccess
      case None =>
        for
          res  <- env.mod.search(query)
          page <- renderPage(views.mod.search(ModUserSearch.form.fill(query), res.some))
        yield Ok(page)

  def print(fh: String) = SecureBody(_.ViewPrintNoIP) { ctx ?=> me ?=>
    val hash = FingerHash(fh)
    for
      uids       <- env.security.api.recentUserIdsByFingerHash(hash)
      users      <- env.user.repo.usersFromSecondary(uids.reverse)
      withEmails <- env.user.api.withPerfsAndEmails(users)
      uas        <- env.security.api.printUas(hash)
      page <- renderPage(views.mod.search.print(hash, withEmails, uas, env.security.printBan.blocks(hash)))
    yield Ok(page)
  }

  def printBan(v: Boolean, fh: String) = Secure(_.PrintBan) { _ ?=> me ?=>
    val hash = FingerHash(fh)
    for _ <- env.security.printBan.toggle(hash, v) yield Redirect(routes.Mod.print(fh))
  }

  def singleIp(ip: String) = SecureBody(_.ViewPrintNoIP) { ctx ?=> me ?=>
    given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply
    env.mod.ipRender.decrypt(ip).so { address =>
      for
        uids       <- env.security.api.recentUserIdsByIp(address)
        users      <- env.user.repo.usersFromSecondary(uids.reverse)
        withEmails <- env.user.api.withPerfsAndEmails(users)
        uas        <- env.security.api.ipUas(address)
        data       <- env.security.ipTrust.data(address)
        blocked = env.security.firewall.blocksIp(address)
        page <- renderPage(views.mod.search.ip(address, withEmails, uas, data, blocked))
      yield Ok(page)
    }
  }

  def singleIpBan(v: Boolean, ip: String) = Secure(_.IpBan) { ctx ?=> me ?=>
    val op =
      if v then env.security.firewall.blockIps
      else env.security.firewall.unblockIps
    val ipAddr = IpAddress.from(ip)
    op(ipAddr.toList).inject:
      if HTTPRequest.isXhr(ctx.req) then jsonOkResult
      else Redirect(routes.Mod.singleIp(ip))
  }

  def blankPassword(username: UserStr) = Secure(_.SetEmail) { _ ?=> _ ?=>
    for
      _ <- env.mod.api.blankPassword(username)
      _ <- env.security.store.closeAllSessionsOf(username.id)
    yield Redirect(routes.User.show(username)).flashSuccess("Password blanked")
  }

  def chatUser(username: UserStr) = SecureOrScoped(_.ChatTimeout) { _ ?=> _ ?=>
    JsonOptionOk:
      env.chat.api.userChat
        .userModInfo(username)
        .map2(lila.chat.JsonView.userModInfo(using env.user.lightUserSync))
  }

  def permissions(username: UserStr) = Secure(_.ChangePermission) { _ ?=> _ ?=>
    FoundPage(env.user.repo.byId(username)):
      views.mod.permissions(_)
  }

  def savePermissions(username: UserStr) = SecureBody(_.ChangePermission) { ctx ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      bindForm(lila.security.Permission.form)(
        _ => BadRequest.page(views.mod.permissions(user)),
        permissions =>
          val newPermissions = Permission.ofDbKeys(permissions).diff(Permission(user))
          (api.setPermissions(user.username, Permission.ofDbKeys(permissions)) >> {
            newPermissions(Permission.Coach).so(env.mailer.automaticEmail.onBecomeCoach(user))
          } >> {
            Permission
              .ofDbKeys(permissions)
              .exists(p =>
                p.grants(Permission.SeeReport) || p.grants(Permission.Developer) || p.grants(
                  Permission.ContentTeam
                ) || p.grants(Permission.BroadcastTeam)
              )
              .so(env.plan.api.setLifetime(user))
          }).inject(Redirect(routes.Mod.permissions(user.username)).flashSuccess)
      )
  }

  def emailConfirm = SecureBody(_.SetEmail) { ctx ?=> me ?=>
    get("q") match
      case None => Ok.page(views.mod.ui.emailConfirm("", none, none))
      case Some(rawQuery) =>
        val query    = rawQuery.trim.split(' ').toList
        val email    = query.headOption.flatMap(EmailAddress.from)
        val username = query.lift(1)
        def tryWith(setEmail: EmailAddress, q: String): Fu[Option[Result]] =
          env.mod
            .search(q)
            .map(_.users.filter(_.user.enabled.yes))
            .flatMap:
              case List(lila.user.WithPerfsAndEmails(user, _)) =>
                for
                  _ <- (!user.everLoggedIn).so:
                    lila.mon.user.register.modConfirmEmail.increment()
                    api.setEmail(user.id, setEmail.some)
                  email <- env.user.repo.email(user.id)
                  page  <- renderPage(views.mod.ui.emailConfirm("", user.some, email))
                yield Ok(page).some
              case _ => fuccess(none)
        email
          .so: em =>
            tryWith(em, em.value)
              .orElse(username.so { tryWith(em, _) })
              .recover(lila.db.recoverDuplicateKey(_ => none))
          .getOrElse(BadRequest.page(views.mod.ui.emailConfirm(rawQuery, none, none)))
  }

  def presets(group: String) = Secure(_.Presets) { ctx ?=> _ ?=>
    env.mod.presets
      .get(group)
      .fold(notFound): setting =>
        Ok.page(views.mod.ui.presets(group, setting.form))
  }

  def presetsUpdate(group: String) = SecureBody(_.Presets) { ctx ?=> _ ?=>
    Found(env.mod.presets.get(group)): setting =>
      bindForm(setting.form)(
        err => BadRequest.page(views.mod.ui.presets(group, err)),
        v => setting.setString(v.toString).inject(Redirect(routes.Mod.presets(group)).flashSuccess)
      )
  }

  def eventStream = SecuredScoped(_.Admin) { _ ?=> _ ?=>
    noProxyBuffer(Ok.chunked(env.mod.stream.events()))
  }

  def markedUsersStream = Scoped() { _ ?=> me ?=>
    me.is(UserId.explorer)
      .so(getTimestamp("since"))
      .so: since =>
        noProxyBuffer(Ok.chunked(env.mod.stream.markedSince(since).map(_.value + "\n")))
  }

  def apiUserLog(username: UserStr) = SecuredScoped(_.ModLog) { _ ?=> me ?=>
    import lila.common.Json.given
    Found(env.user.repo.byId(username)): user =>
      for
        logs      <- env.mod.logApi.userHistory(user.id)
        notes     <- env.user.noteApi.getForMyPermissions(user)
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
    env.report.api.getSuspect(username).flatMapz(f)

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
