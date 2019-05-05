package lila.mod

import lila.common.{ IpAddress, EmailAddress }
import lila.report.{ Mod, ModId, Suspect, SuspectId, Room }
import lila.security.{ Permission, Granter }
import lila.security.{ Firewall, UserSpy, Store => SecurityStore }
import lila.user.{ User, UserRepo, Title, LightUserApi }

final class ModApi(
    logApi: ModlogApi,
    userSpy: User => Fu[UserSpy],
    firewall: Firewall,
    reporter: akka.actor.ActorSelection,
    reportApi: lila.report.ReportApi,
    notifier: ModNotifier,
    lightUserApi: LightUserApi,
    refunder: RatingRefund,
    lilaBus: lila.common.Bus
) {

  def setEngine(mod: Mod, prev: Suspect, v: Boolean): Funit = (prev.user.engine != v) ?? {
    for {
      _ <- UserRepo.setEngine(prev.user.id, v)
      sus = prev.set(_.copy(engine = v))
      _ <- reportApi.process(mod, sus, Set(Room.Cheat, Room.Print))
      _ <- logApi.engine(mod, sus, v)
    } yield {
      lilaBus.publish(lila.hub.actorApi.mod.MarkCheater(sus.user.id, v), 'adjustCheater)
      if (v) {
        notifier.reporters(mod, sus)
        refunder schedule sus
      }
    }
  }

  def autoMark(suspectId: SuspectId, modId: ModId): Funit = for {
    sus <- reportApi.getSuspect(suspectId.value) flatten s"No such suspect $suspectId"
    unengined <- logApi.wasUnengined(sus)
    _ <- (!sus.user.isBot && !unengined) ?? {
      reportApi.getMod(modId.value) flatMap {
        _ ?? { mod =>
          lila.mon.cheat.autoMark.count()
          setEngine(mod, sus, true)
        }
      }
    }
  } yield ()

  def setBooster(mod: Mod, prev: Suspect, v: Boolean): Fu[Suspect] =
    if (prev.user.booster == v) fuccess(prev)
    else for {
      _ <- UserRepo.setBooster(prev.user.id, v)
      sus = prev.set(_.copy(booster = v))
      _ <- reportApi.process(mod, sus, Set(Room.Other))
      _ <- logApi.booster(mod, sus, v)
    } yield {
      if (v) {
        lilaBus.publish(lila.hub.actorApi.mod.MarkBooster(sus.user.id), 'adjustBooster)
        notifier.reporters(mod, sus)
      }
      sus
    }

  def autoBooster(winnerId: User.ID, loserId: User.ID): Funit =
    logApi.wasUnbooster(loserId) map {
      case false => reporter ! lila.hub.actorApi.report.Booster(winnerId, loserId)
      case true => ()
    }

  def setTroll(mod: Mod, prev: Suspect, value: Boolean): Fu[Suspect] = {
    val changed = value != prev.user.troll
    val sus = prev.set(_.copy(troll = value))
    changed ?? {
      UserRepo.updateTroll(sus.user).void >>- {
        logApi.troll(mod, sus)
        lilaBus.publish(lila.hub.actorApi.mod.Shadowban(sus.user.id, value), 'shadowban)
      }
    } >>
      reportApi.process(mod, sus, Set(Room.Coms)) >>- {
        if (value) notifier.reporters(mod, sus)
      } inject sus
  }

  def setBan(mod: Mod, prev: Suspect, value: Boolean): Funit = for {
    spy <- userSpy(prev.user)
    sus = prev.set(_.copy(ipBan = value))
    _ <- UserRepo.setIpBan(sus.user.id, sus.user.ipBan)
    _ <- logApi.ban(mod, sus)
    _ <- if (sus.user.ipBan) firewall.blockIps(spy.rawIps) >> SecurityStore.disconnect(sus.user.id)
    else firewall unblockIps spy.rawIps
  } yield ()

  def garbageCollect(sus: Suspect, ipBan: Boolean): Funit = for {
    mod <- reportApi.getLichessMod
    _ <- setEngine(mod, sus, true)
    _ <- ipBan ?? setBan(mod, sus, true)
  } yield logApi.garbageCollect(mod, sus)

  def disableTwoFactor(mod: String, username: String): Funit = withUser(username) { user =>
    (UserRepo disableTwoFactor user.id) >> logApi.disableTwoFactor(mod, user.id)
  }

  def closeAccount(mod: String, username: String): Fu[Option[User]] = withUser(username) { user =>
    user.enabled ?? {
      logApi.closeAccount(mod, user.id) inject user.some
    }
  }

  def reopenAccount(mod: String, username: String): Funit = withUser(username) { user =>
    !user.enabled ?? {
      (UserRepo reopen user.id) >> logApi.reopenAccount(mod, user.id)
    }
  }

  def setTitle(mod: String, username: String, title: Option[Title]): Funit = withUser(username) { user =>
    title match {
      case None => {
        UserRepo.removeTitle(user.id) >>-
          logApi.removeTitle(mod, user.id) >>-
          lightUserApi.invalidate(user.id)
      }
      case Some(t) => Title.names.get(t) ?? { tFull =>
        UserRepo.addTitle(user.id, t) >>-
          logApi.addTitle(mod, user.id, s"$t ($tFull)") >>-
          lightUserApi.invalidate(user.id)
      }
    }
  }

  def setEmail(mod: String, username: String, email: EmailAddress): Funit = withUser(username) { user =>
    UserRepo.setEmail(user.id, email) >>
      UserRepo.setEmailConfirmed(user.id) >>
      logApi.setEmail(mod, user.id)
  }

  def setPermissions(mod: Mod, username: String, permissions: Set[Permission]): Funit = withUser(username) { user =>
    val finalPermissions = Permission(user.roles).filter { p =>
      // only remove permissions the mod can actually grant
      permissions.contains(p) || !Granter.canGrant(mod.user, p)
    } ++
      // only add permissions the mod can actually grant
      permissions.filter(Granter.canGrant(mod.user, _))
    UserRepo.setRoles(user.id, finalPermissions.map(_.name).toList) >> {
      lilaBus.publish(lila.hub.actorApi.mod.SetPermissions(user.id, finalPermissions.map(_.name).toList), 'setPermissions)
      logApi.setPermissions(mod, user.id, permissions.toList)
    }
  }

  def setReportban(mod: Mod, sus: Suspect, v: Boolean): Funit = (sus.user.reportban != v) ?? {
    UserRepo.setReportban(sus.user.id, v) >>- logApi.reportban(mod, sus, v)
  }

  def setRankban(mod: Mod, sus: Suspect, v: Boolean): Funit = (sus.user.rankban != v) ?? {
    if (v) lilaBus.publish(lila.hub.actorApi.mod.KickFromRankings(sus.user.id), 'kickFromRankings)
    UserRepo.setRankban(sus.user.id, v) >>- logApi.rankban(mod, sus, v)
  }

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op
}
