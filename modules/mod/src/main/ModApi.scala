package lila.mod

import lila.common.{ Bus, EmailAddress }
import lila.report.{ Mod, ModId, Room, Suspect, SuspectId }
import lila.security.{ Granter, Permission }
import lila.user.{ Holder, LightUserApi, Title, User, UserRepo }

final class ModApi(
    userRepo: UserRepo,
    logApi: ModlogApi,
    reportApi: lila.report.ReportApi,
    noteApi: lila.user.NoteApi,
    notifier: ModNotifier,
    lightUserApi: LightUserApi,
    refunder: RatingRefund
)(using Executor):

  def setAlt(mod: Mod, prev: Suspect, v: Boolean): Funit =
    for {
      _ <- userRepo.setAlt(prev.user.id, v)
      sus = prev.set(_.withMarks(_.set(_.Alt, v)))
      _ <- logApi.alt(mod, sus, v)
    } yield if (v) notifier.reporters(mod, sus).unit

  def setEngine(mod: Mod, prev: Suspect, v: Boolean): Funit =
    (prev.user.marks.engine != v) ?? {
      for {
        _ <- userRepo.setEngine(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.Engine, v)))
        _ <- logApi.engine(mod, sus, v)
      } yield
        Bus.publish(lila.hub.actorApi.mod.MarkCheater(sus.user.id, v), "adjustCheater")
        if (v)
          notifier.reporters(mod, sus)
          refunder schedule sus
    }

  def autoMark(suspectId: SuspectId, modId: ModId, note: String): Funit =
    for {
      sus       <- reportApi.getSuspect(suspectId.value) orFail s"No such suspect $suspectId"
      unengined <- logApi.wasUnengined(sus)
      _ <- (!sus.user.isBot && !sus.user.marks.engine && !unengined) ?? {
        reportApi.getMod(modId) flatMapz { mod =>
          lila.mon.cheat.autoMark.increment()
          setEngine(mod, sus, v = true) >>
          noteApi.lichessWrite(sus.user, note) >>
          reportApi.autoProcess(modId, sus, Set(Room.Cheat, Room.Print))  
        }
      }
    } yield ()

  def setBoost(mod: Mod, prev: Suspect, v: Boolean): Fu[Suspect] =
    if (prev.user.marks.boost == v) fuccess(prev)
    else
      for {
        _ <- userRepo.setBoost(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.Boost, v)))
        _ <- logApi.booster(mod, sus, v)
      } yield
        if (v)
          Bus.publish(lila.hub.actorApi.mod.MarkBooster(sus.user.id), "adjustBooster")
          notifier.reporters(mod, sus)
        sus

  def setTroll(mod: Mod, prev: Suspect, value: Boolean): Fu[Suspect] =
    val changed = value != prev.user.marks.troll
    val sus     = prev.set(_.withMarks(_.set(_.Troll, value)))
    changed ?? {
      userRepo.updateTroll(sus.user).void >>- {
        logApi.troll(mod, sus)
        Bus.publish(lila.hub.actorApi.mod.Shadowban(sus.user.id, value), "shadowban")
      }
    } >>- {
      if (value) notifier.reporters(mod, sus).unit
    } inject sus

  def autoTroll(sus: Suspect, note: String): Funit =
    reportApi.getLichessMod flatMap { mod =>
      setTroll(mod, sus, true) >>
        noteApi.lichessWrite(sus.user, note)
        >> reportApi.autoProcess(mod.id, sus, Set(Room.Comm))
    }

  def garbageCollect(userId: UserId): Funit = for {
    sus <- reportApi getSuspect userId orFail s"No such suspect $userId"
    mod <- reportApi.getLichessMod
    _   <- setAlt(mod, sus, v = true)
    _   <- logApi.garbageCollect(mod, sus)
  } yield ()

  def disableTwoFactor(mod: ModId, username: UserStr): Funit =
    withUser(username) { user =>
      (userRepo disableTwoFactor user.id) >> logApi.disableTwoFactor(mod, user.id)
    }

  def reopenAccount(mod: ModId, username: UserStr): Funit =
    withUser(username) { user =>
      user.enabled.no ?? {
        (userRepo reopen user.id) >> logApi.reopenAccount(mod, user.id)
      }
    }

  def setKid(mod: ModId, username: UserStr): Funit =
    withUser(username) { user =>
      userRepo.isKid(user.id) flatMap {
        !_ ?? { (userRepo.setKid(user, true)) } >> logApi.setKidMode(mod, user.id)
      }
    }

  def setTitle(mod: ModId, username: UserStr, title: Option[UserTitle]): Funit =
    withUser(username) { user =>
      title match
        case None =>
          userRepo.removeTitle(user.id) >>
            logApi.removeTitle(mod, user.id) >>-
            lightUserApi.invalidate(user.id)
        case Some(t) =>
          Title.names.get(t) ?? { tFull =>
            userRepo.addTitle(user.id, t) >>
              logApi.addTitle(mod, user.id, s"$t ($tFull)") >>-
              lightUserApi.invalidate(user.id)
          }
    }

  def setEmail(mod: ModId, username: UserStr, email: EmailAddress): Funit =
    withUser(username) { user =>
      userRepo.setEmail(user.id, email) >>
        userRepo.setEmailConfirmed(user.id) >>
        logApi.setEmail(mod, user.id)
    }

  def setPermissions(mod: Holder, username: UserStr, permissions: Set[Permission]): Funit =
    withUser(username) { user =>
      val finalPermissions = Permission(user.roles).filter { p =>
        // only remove permissions the mod can actually grant
        permissions.contains(p) || !Granter.canGrant(mod, p)
      } ++
        // only add permissions the mod can actually grant
        permissions.filter(Granter.canGrant(mod, _))
      userRepo.setRoles(user.id, finalPermissions.map(_.dbKey).toList) >>
        logApi.setPermissions(
          Mod.holder(mod),
          user.id,
          Permission.diff(Permission(user.roles), finalPermissions)
        )
    }

  def setReportban(mod: Mod, sus: Suspect, v: Boolean): Funit =
    (sus.user.marks.reportban != v) ?? {
      userRepo.setReportban(sus.user.id, v) >> logApi.reportban(mod, sus, v)
    }

  def setRankban(mod: Mod, sus: Suspect, v: Boolean): Funit =
    (sus.user.marks.rankban != v) ?? {
      if (v) Bus.publish(lila.hub.actorApi.mod.KickFromRankings(sus.user.id), "kickFromRankings")
      userRepo.setRankban(sus.user.id, v) >> logApi.rankban(mod, sus, v)
    }

  def allMods =
    userRepo.userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList) flatMap
      userRepo.enabledByIds dmap {
        _.sortBy(_.timeNoSee)
      }

  private def withUser[A](username: UserStr)(op: User => Fu[A]): Fu[A] =
    userRepo byId username orFail s"[mod] missing user $username" flatMap op
