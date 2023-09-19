package lila.mod

import lila.common.{ Bus, EmailAddress }
import lila.report.{ Mod, ModId, Room, Suspect, SuspectId }
import lila.security.{ Granter, Permission }
import lila.user.{ Me, LightUserApi, Title, User, UserRepo }

final class ModApi(
    userRepo: UserRepo,
    logApi: ModlogApi,
    reportApi: lila.report.ReportApi,
    noteApi: lila.user.NoteApi,
    notifier: ModNotifier,
    lightUserApi: LightUserApi,
    refunder: RatingRefund
)(using Executor):

  def setAlt(prev: Suspect, v: Boolean)(using me: Me.Id): Funit =
    for
      _ <- userRepo.setAlt(prev.user.id, v)
      sus = prev.set(_.withMarks(_.set(_.Alt, v)))
      _ <- logApi.alt(sus, v)
    yield if v then notifier.reporters(me.modId, sus)

  def setEngine(prev: Suspect, v: Boolean)(using me: Me.Id): Funit =
    (prev.user.marks.engine != v) so {
      for
        _ <- userRepo.setEngine(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.Engine, v)))
        _ <- logApi.engine(sus, v)
      yield
        Bus.publish(lila.hub.actorApi.mod.MarkCheater(sus.user.id, v), "adjustCheater")
        if v then
          notifier.reporters(me.modId, sus)
          refunder schedule sus
    }

  def autoMark(suspectId: SuspectId, note: String)(using Me.Id): Funit =
    for
      sus       <- reportApi.getSuspect(suspectId.value) orFail s"No such suspect $suspectId"
      unengined <- logApi.wasUnengined(sus)
      _ <- (!sus.user.isBot && !sus.user.marks.engine && !unengined) so {
        reportApi.getMyMod.flatMapz: mod =>
          lila.mon.cheat.autoMark.increment()
          setEngine(sus, v = true) >>
            noteApi.lichessWrite(sus.user, note) >>
            reportApi.autoProcess(sus, Set(Room.Cheat, Room.Print))
      }
    yield ()

  def setBoost(prev: Suspect, v: Boolean)(using me: Me): Fu[Suspect] =
    if prev.user.marks.boost == v then fuccess(prev)
    else
      for
        _ <- userRepo.setBoost(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.Boost, v)))
        _ <- logApi.booster(sus, v)
      yield
        if v then
          Bus.publish(lila.hub.actorApi.mod.MarkBooster(sus.user.id), "adjustBooster")
          notifier.reporters(me.modId, sus)
        sus

  def setTroll(prev: Suspect, value: Boolean)(using me: Me.Id): Fu[Suspect] =
    val changed = value != prev.user.marks.troll
    val sus     = prev.set(_.withMarks(_.set(_.Troll, value)))
    changed
      .so:
        userRepo.updateTroll(sus.user).void andDo {
          logApi.troll(sus)
          Bus.publish(lila.hub.actorApi.mod.Shadowban(sus.user.id, value), "shadowban")
        }
      .andDo:
        if value then notifier.reporters(me.modId, sus)
      .inject(sus)

  def autoTroll(sus: Suspect, note: String): Funit =
    given Me.Id = User.lichessIdAsMe
    setTroll(sus, true) >>
      noteApi.lichessWrite(sus.user, note)
      >> reportApi.autoProcess(sus, Set(Room.Comm))

  def garbageCollect(userId: UserId): Funit =
    given Me.Id = User.lichessIdAsMe
    for
      sus <- reportApi getSuspect userId orFail s"No such suspect $userId"
      _   <- setAlt(sus, v = true)
      _   <- logApi.garbageCollect(sus)
    yield ()

  def disableTwoFactor(mod: ModId, username: UserStr): Funit =
    withUser(username): user =>
      (userRepo disableTwoFactor user.id) >> logApi.disableTwoFactor(mod, user.id)

  def reopenAccount(username: UserStr)(using Me): Funit =
    withUser(username): user =>
      user.enabled.no.so:
        userRepo.reopen(user.id) >> logApi.reopenAccount(user.id)

  def setKid(mod: ModId, username: UserStr): Funit =
    withUser(username): user =>
      userRepo.isKid(user.id) flatMap {
        !_ so { (userRepo.setKid(user, true)) } >> logApi.setKidMode(mod, user.id)
      }

  def setTitle(username: UserStr, title: Option[UserTitle])(using Me): Funit =
    withUser(username): user =>
      title match
        case None =>
          userRepo.removeTitle(user.id) >>
            logApi.removeTitle(user.id) andDo
            lightUserApi.invalidate(user.id)
        case Some(t) =>
          Title.names.get(t) so { tFull =>
            userRepo.addTitle(user.id, t) >>
              logApi.addTitle(user.id, s"$t ($tFull)") andDo
              lightUserApi.invalidate(user.id)
          }

  def setEmail(username: UserStr, email: EmailAddress)(using Me): Funit =
    withUser(username): user =>
      userRepo.setEmail(user.id, email) >>
        userRepo.setEmailConfirmed(user.id) >>
        logApi.setEmail(user.id)

  def setPermissions(username: UserStr, permissions: Set[Permission])(using Me): Funit =
    withUser(username): user =>
      val finalPermissions = Permission(user.roles).filter { p =>
        // only remove permissions the mod can actually grant
        permissions.contains(p) || !Granter.canGrant(p)
      } ++
        // only add permissions the mod can actually grant
        permissions.filter(Granter.canGrant)
      userRepo.setRoles(user.id, finalPermissions.map(_.dbKey).toList) >>
        logApi.setPermissions(
          user.id,
          Permission.diff(Permission(user.roles), finalPermissions)
        )

  def setReportban(sus: Suspect, v: Boolean)(using Me.Id): Funit =
    (sus.user.marks.reportban != v) so {
      userRepo.setReportban(sus.user.id, v) >> logApi.reportban(sus, v)
    }

  def setRankban(sus: Suspect, v: Boolean)(using Me.Id): Funit =
    (sus.user.marks.rankban != v) so {
      if v then Bus.publish(lila.hub.actorApi.mod.KickFromRankings(sus.user.id), "kickFromRankings")
      userRepo.setRankban(sus.user.id, v) >> logApi.rankban(sus, v)
    }

  def setArenaBan(sus: Suspect, v: Boolean)(using Me.Id): Funit =
    (sus.user.marks.arenaBan != v) so {
      userRepo.setArenaBan(sus.user.id, v) >> logApi.arenaBan(sus, v)
    }

  def setPrizeban(sus: Suspect, v: Boolean)(using Me.Id): Funit =
    (sus.user.marks.prizeban != v) so {
      userRepo.setPrizeban(sus.user.id, v) >> logApi.prizeban(sus, v)
    }

  def allMods =
    userRepo.userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList) flatMap
      userRepo.enabledByIds dmap {
        _.sortBy(_.timeNoSee)
      }

  private def withUser[A](username: UserStr)(op: User => Fu[A]): Fu[A] =
    userRepo byId username orFail s"[mod] missing user $username" flatMap op
