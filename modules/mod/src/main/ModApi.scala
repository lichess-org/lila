package lila.mod

import lila.common.{ Bus, EmailAddress }
import lila.report.{ Mod, ModId, Room, Suspect, SuspectId }
import lila.security.{ Granter, Permission }
import lila.user.{ LightUserApi, Title, User, UserRepo }

final class ModApi(
    userRepo: UserRepo,
    logApi: ModlogApi,
    reportApi: lila.report.ReportApi,
    reporter: lila.hub.actors.Report,
    notifier: ModNotifier,
    lightUserApi: LightUserApi,
    refunder: RatingRefund
)(implicit ec: scala.concurrent.ExecutionContext) {

  def setAlt(mod: Mod, prev: Suspect, v: Boolean): Funit =
    for {
      _ <- userRepo.setAlt(prev.user.id, v)
      sus = prev.set(_.withMarks(_.set(_.Alt, v)))
      _ <- reportApi.process(mod, sus, Set(Room.Cheat, Room.Print))
      _ <- logApi.alt(mod, sus, v)
    } yield {
      if (v) notifier.reporters(mod, sus).unit
    }

  def setEngine(mod: Mod, prev: Suspect, v: Boolean): Funit =
    (prev.user.marks.engine != v) ?? {
      for {
        _ <- userRepo.setEngine(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.Engine, v)))
        _ <- reportApi.process(mod, sus, Set(Room.Cheat, Room.Print))
        _ <- logApi.engine(mod, sus, v)
      } yield {
        Bus.publish(lila.hub.actorApi.mod.MarkCheater(sus.user.id, v), "adjustCheater")
        if (v) {
          notifier.reporters(mod, sus)
          refunder schedule sus
        }
      }
    }

  def autoMark(suspectId: SuspectId, modId: ModId): Funit =
    for {
      sus       <- reportApi.getSuspect(suspectId.value) orFail s"No such suspect $suspectId"
      unengined <- logApi.wasUnengined(sus)
      _ <- (!sus.user.isBot && !unengined) ?? {
        reportApi.getMod(modId.value) flatMap {
          _ ?? { mod =>
            lila.mon.cheat.autoMark.increment()
            setEngine(mod, sus, v = true)
          }
        }
      }
    } yield ()

  def setBoost(mod: Mod, prev: Suspect, v: Boolean): Fu[Suspect] =
    if (prev.user.marks.boost == v) fuccess(prev)
    else
      for {
        _ <- userRepo.setBoost(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.Boost, v)))
        _ <- reportApi.process(mod, sus, Set(Room.Other))
        _ <- logApi.booster(mod, sus, v)
      } yield {
        if (v) {
          Bus.publish(lila.hub.actorApi.mod.MarkBooster(sus.user.id), "adjustBooster")
          notifier.reporters(mod, sus)
        }
        sus
      }

  def autoBoost(winnerId: User.ID, loserId: User.ID): Funit =
    logApi.wasUnbooster(loserId) map {
      case false => reporter ! lila.hub.actorApi.report.Booster(winnerId, loserId)
      case true  => ()
    }

  def setTroll(mod: Mod, prev: Suspect, value: Boolean): Fu[Suspect] = {
    val changed = value != prev.user.marks.troll
    val sus     = prev.set(_.withMarks(_.set(_.Troll, value)))
    changed ?? {
      userRepo.updateTroll(sus.user).void >>- {
        logApi.troll(mod, sus)
        Bus.publish(lila.hub.actorApi.mod.Shadowban(sus.user.id, value), "shadowban")
      }
    } >>
      reportApi.process(mod, sus, Set(Room.Comm)) >>- {
        if (value) notifier.reporters(mod, sus).unit
      } inject sus
  }

  def garbageCollect(sus: Suspect): Funit =
    reportApi.getLichessMod flatMap { mod =>
      setAlt(mod, sus, v = true) >>
        setTroll(mod, sus, value = false) >>
        logApi.garbageCollect(mod, sus)
    }

  def disableTwoFactor(mod: String, username: String): Funit =
    withUser(username) { user =>
      (userRepo disableTwoFactor user.id) >> logApi.disableTwoFactor(mod, user.id)
    }

  def reopenAccount(mod: String, username: String): Funit =
    withUser(username) { user =>
      !user.enabled ?? {
        (userRepo reopen user.id) >> logApi.reopenAccount(mod, user.id)
      }
    }

  def setKid(mod: String, username: String): Funit =
    withUser(username) { user =>
      userRepo.isKid(user.id) flatMap {
        !_ ?? {
          (userRepo.setKid(user, true)) >> logApi.reopenAccount(mod, user.id)
        }
      }
    }  

  def setTitle(mod: String, username: String, title: Option[Title]): Funit =
    withUser(username) { user =>
      title match {
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
    }

  def setEmail(mod: String, username: String, email: EmailAddress): Funit =
    withUser(username) { user =>
      userRepo.setEmail(user.id, email) >>
        userRepo.setEmailConfirmed(user.id) >>
        logApi.setEmail(mod, user.id)
    }

  def setPermissions(mod: Mod, username: String, permissions: Set[Permission]): Funit =
    withUser(username) { user =>
      val finalPermissions = Permission(user.roles).filter { p =>
        // only remove permissions the mod can actually grant
        permissions.contains(p) || !Granter.canGrant(mod.user, p)
      } ++
        // only add permissions the mod can actually grant
        permissions.filter(Granter.canGrant(mod.user, _))
      userRepo.setRoles(user.id, finalPermissions.map(_.dbKey).toList) >> {
        Bus.publish(
          lila.hub.actorApi.mod.SetPermissions(user.id, finalPermissions.map(_.dbKey).toList),
          "setPermissions"
        )
        logApi.setPermissions(mod, user.id, permissions.toList)
      }
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

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    userRepo named username orFail s"[mod] missing user $username" flatMap op
}
