package lila.mod

import chess.PlayerTitle

import lila.common.Bus
import lila.core.perm.Permission
import lila.core.report.SuspectId
import lila.core.user.{ UserMark, UserMarks, KidMode }
import lila.report.{ Room, Suspect }
import lila.user.{ LightUserApi, UserRepo, UserApi }

final class ModApi(
    userRepo: UserRepo,
    userApi: UserApi,
    logApi: ModlogApi,
    reportApi: lila.report.ReportApi,
    noteApi: lila.user.NoteApi,
    prefApi: lila.core.pref.PrefApi,
    notifier: ModNotifier,
    lightUserApi: LightUserApi,
    refunder: RatingRefund
)(using Executor)
    extends lila.core.mod.ModApi:

  extension (a: UserMarks)
    def set(sel: UserMark.type => UserMark, v: Boolean) = UserMarks:
      if v then sel(UserMark) :: a.value
      else a.value.filter(sel(UserMark) !=)

  def setAlt(prev: Suspect, v: Boolean)(using me: MyId): Fu[Suspect] =
    for
      _ <- userRepo.setAlt(prev.user.id, v)
      sus = prev.set(_.withMarks(_.set(_.alt, v)))
      _ <- logApi.alt(sus, v)
      _ = if v then notifier.actionTaken(me.modId, sus, Room.Other)
    yield sus

  def afterModClose(u: User)(using me: Me) =
    notifier.actionTaken(me.modId, Suspect(u), Room.Other)

  def afterWarning(sus: Suspect)(using me: Me) =
    reportApi.inquiries
      .ofModId(me)
      .map: inquiry =>
        val room = inquiry.fold(Room.Comm)(_.room)
        notifier.actionTaken(me.modId, sus, room)

  def setEngine(prev: Suspect, v: Boolean)(using me: MyId): Funit =
    (prev.user.marks.engine != v).so:
      for
        _ <- userRepo.setEngine(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.engine, v)))
        _ <- logApi.engine(sus, v)
      yield
        Bus.pub(lila.core.mod.MarkCheater(sus.user.id, v))
        if v then
          notifier.actionTaken(me.modId, sus, Room.Cheat)
          refunder.schedule(sus)

  def autoEngine(suspectId: SuspectId, note: String)(using MyId): Funit =
    for
      sus <- reportApi.getSuspect(suspectId.value).orFail(s"No such suspect $suspectId")
      unengined <- logApi.wasUnengined(sus)
      closedReports <- reportApi.countClosedAutoCheatReport(sus.user.id)
      _ <- (!sus.user.isBot && !sus.user.marks.engine && !unengined && closedReports < 2).so:
        for
          _ <- setEngine(sus, v = true)
          _ <- noteApi.lichessWrite(sus.user, note)
          _ <- reportApi.autoProcess(sus, Set(Room.Cheat, Room.Print))
          _ = lila.mon.cheat.autoMark.increment()
          _ = notifier.actionTaken(ModId.lichess, sus, Room.Cheat)
        yield ()
    yield ()

  def setBoost(prev: Suspect, v: Boolean)(using me: Me): Fu[Suspect] =
    if prev.user.marks.boost == v then fuccess(prev)
    else
      for
        _ <- userRepo.setBoost(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.boost, v)))
        _ <- logApi.booster(sus, v)
      yield
        Bus.pub(lila.core.mod.MarkBooster(sus.user.id, v))
        if v then notifier.actionTaken(me.modId, sus, Room.Boost)
        sus

  def setTroll(prev: Suspect, value: Boolean)(using me: MyId): Fu[Suspect] =
    if !value && prev.user.marks.isolate
    then setIsolate(prev, value).flatMap(setTroll(_, value))
    else
      val changed = value != prev.user.marks.troll
      val sus = prev.set(_.withMarks(_.set(_.troll, value)))
      for
        _ <- changed.so:
          for _ <- userRepo.updateTroll(sus.user)
          yield
            logApi.troll(sus)
            Bus.pub(lila.core.mod.Shadowban(sus.user.id, value))
        _ = if value then notifier.actionTaken(me.modId, sus, Room.Comm)
      yield sus

  def autoTroll(sus: Suspect, note: String): Funit =
    given me: MyId = UserId.lichessAsMe
    for
      _ <- setTroll(sus, true)
      _ <- noteApi.lichessWrite(sus.user, note)
      _ <- reportApi.autoProcess(sus, Set(Room.Comm))
      _ = notifier.actionTaken(ModId.lichess, sus, Room.Comm)
    yield ()

  def setIsolate(prev: Suspect, value: Boolean)(using me: MyId): Fu[Suspect] =
    if value && !prev.user.marks.troll
    then setTroll(prev, value).flatMap(setIsolate(_, value))
    else
      val changed = value != prev.user.marks.isolate
      val sus = prev.set(_.withMarks(_.set(_.isolate, value)))
      changed
        .so:
          for
            _ <- userRepo.setIsolate(sus.user.id, value)
            _ <- prefApi.isolate(sus.user)
            _ = logApi.isolate(sus)
          yield ()
        .inject(sus)

  def garbageCollect(userId: UserId): Funit =
    given MyId = UserId.lichessAsMe
    for
      sus <- reportApi.getSuspect(userId).orFail(s"No such suspect $userId")
      _ <- setAlt(sus, v = true)
      _ <- logApi.garbageCollect(sus)
    yield ()

  def disableTwoFactor(mod: ModId, username: UserStr): Funit =
    withUser(username): user =>
      (userRepo.disableTwoFactor(user.id)) >> logApi.disableTwoFactor(mod, user.id)

  def reopenAccount(username: UserStr)(using Me): Funit =
    withUser(username): user =>
      user.enabled.no.so:
        for _ <- userRepo.reopen(user.id)
        yield
          Bus.pub(lila.core.security.ReopenAccount(user))
          logApi.reopenAccount(user.id)

  def setKid(mod: ModId, username: UserStr, v: KidMode): Funit =
    withUser(username): user =>
      userApi
        .setKid(user, v)
        .flatMapz: mode =>
          mode.yes.so(notifier.notifyKidMode(mod, user)) >>
            logApi.setKidMode(mod, user.id, mode)

  def setTitle(username: UserStr, title: Option[PlayerTitle])(using Me): Funit =
    withUser(username): user =>
      title match
        case None =>
          for
            _ <- userRepo.removeTitle(user.id)
            _ <- logApi.removeTitle(user.id)
          yield lightUserApi.invalidate(user.id)
        case Some(t) =>
          PlayerTitle.names.get(t).so { tFull =>
            for
              _ <- userRepo.setTitle(user.id, t)
              _ <- logApi.setTitle(user.id, s"$t ($tFull)")
            yield lightUserApi.invalidate(user.id)
          }

  def setEmail(username: UserStr, emailOpt: Option[EmailAddress])(using Me): Funit =
    withUser(username): user =>
      for
        prev <- userRepo.emailOrPrevious(user.id)
        email = emailOpt | EmailAddress:
          s"noreply.blanked.${username.id}${prev.fold("@nope.nope")("." + _)}"
        _ <- userRepo.setEmail(user.id, email)
        _ <- userRepo.setEmailConfirmed(user.id)
        _ <- logApi.setEmail(user.id, prev, email)
      yield ()

  def blankPassword(username: UserStr)(using Me): Funit =
    withUser(username): user =>
      for
        _ <- userRepo.blankPassword(user.id)
        _ <- logApi.blankPassword(user.id)
      yield ()

  def setPermissions(username: UserStr, permissions: Set[Permission])(using Me): Funit =
    withUser(username): user =>
      val finalPermissions = Permission(user).filter { p =>
        // only remove permissions the mod can actually grant
        permissions.contains(p) || !canGrant(p)
      } ++
        // only add permissions the mod can actually grant
        permissions.filter(canGrant)
      userRepo.setRoles(user.id, finalPermissions.map(_.dbKey).toList) >>
        logApi.setPermissions(user.light, permDiff(Permission(user), finalPermissions))

  private def permDiff(orig: Set[Permission], dest: Set[Permission]): Map[Permission, Boolean] = {
    orig.diff(dest).map(_ -> false) ++ dest.diff(orig).map(_ -> true)
  }.toMap

  def setReportban(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.reportban != v).so:
      Bus.pub(lila.core.mod.ReportBan(sus.user.id, v))
      userRepo.setReportban(sus.user.id, v) >> logApi.reportban(sus, v)

  def setRankban(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.rankban != v).so:
      Bus.pub(lila.core.mod.RankBan(sus.user.id, v))
      userRepo.setRankban(sus.user.id, v) >> logApi.rankban(sus, v)

  def setArenaBan(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.arenaBan != v).so:
      Bus.pub(lila.core.mod.ArenaBan(sus.user.id, v))
      userRepo.setArenaBan(sus.user.id, v) >> logApi.arenaBan(sus, v)

  def setPrizeban(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.prizeban != v).so:
      Bus.pub(lila.core.mod.PrizeBan(sus.user.id, v))
      userRepo.setPrizeban(sus.user.id, v) >> logApi.prizeban(sus, v)

  def allMods =
    def timeNoSee(u: User): Duration = (nowMillis - (u.seenAt | u.createdAt).toMillis).millis
    userRepo
      .userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList)
      .flatMap(userRepo.enabledByIds)
      .map(_.sortBy(timeNoSee))

  private def withUser[A](username: UserStr)(op: User => Fu[A]): Fu[A] =
    userRepo.byId(username).orFail(s"[mod] missing user $username").flatMap(op)
