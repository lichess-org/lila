package lila.mod

import chess.Color
import lila.db.dsl._
import lila.security.{ Firewall, UserSpy, Store => SecurityStore }
import lila.user.{ User, UserRepo, LightUserApi }

final class ModApi(
    logApi: ModlogApi,
    userSpy: String => Fu[UserSpy],
    firewall: Firewall,
    reporter: akka.actor.ActorSelection,
    notifyReporters: NotifyReporters,
    lightUserApi: LightUserApi,
    refunder: RatingRefund,
    lilaBus: lila.common.Bus) {

  def toggleEngine(mod: String, username: String): Funit = withUser(username) { user =>
    setEngine(mod, username, !user.engine)
  }

  def setEngine(mod: String, username: String, v: Boolean): Funit = withUser(username) { user =>
    (user.engine != v) ?? {
      logApi.engine(mod, user.id, v) zip
        UserRepo.setEngine(user.id, v) >>- {
          if (v) {
            lilaBus.publish(lila.hub.actorApi.mod.MarkCheater(user.id), 'adjustCheater)
            notifyReporters(user)
            refunder schedule user
          }
          reporter ! lila.hub.actorApi.report.MarkCheater(user.id, mod)
        } void
    }
  }

  def autoAdjust(username: String): Funit = logApi.wasUnengined(User.normalize(username)) flatMap {
    case true => funit
    case false =>
      lila.mon.cheat.autoMark.count()
      setEngine("lichess", username, true)
  }

  def toggleBooster(mod: String, username: String): Funit = withUser(username) { user =>
    setBooster(mod, username, !user.booster)
  }

  def setBooster(mod: String, username: String, v: Boolean): Funit = withUser(username) { user =>
    (user.booster != v) ?? {
      logApi.booster(mod, user.id, v) zip
        UserRepo.setBooster(user.id, v) >>- {
          if (v) {
            lilaBus.publish(lila.hub.actorApi.mod.MarkBooster(user.id), 'adjustBooster)
            notifyReporters(user)
          }
        } void
    }
  }

  def autoBooster(userId: String, accomplice: String): Funit =
    logApi.wasUnbooster(userId) map {
      case false => reporter ! lila.hub.actorApi.report.Booster(userId, accomplice)
      case true  =>
    }

  def troll(mod: String, username: String, value: Boolean): Fu[Boolean] = withUser(username) { u =>
    val changed = value != u.troll
    val user = u.copy(troll = value)
    changed ?? {
      UserRepo.updateTroll(user).void >>-
        logApi.troll(mod, user.id, user.troll)
    } >>- {
      if (value) notifyReporters(user)
      (reporter ! lila.hub.actorApi.report.MarkTroll(user.id, mod))
    } inject user.troll
  }

  def ban(mod: String, username: String): Funit = withUser(username) { user =>
    userSpy(user.id) flatMap { spy =>
      UserRepo.toggleIpBan(user.id) zip
        logApi.ban(mod, user.id, !user.ipBan) zip
        user.ipBan.fold(
          firewall unblockIps spy.ipStrings,
          (spy.ipStrings map firewall.blockIp).sequenceFu >>
            (SecurityStore disconnect user.id)
        ) void
    }
  }

  def closeAccount(mod: String, username: String): Fu[Option[User]] = withUser(username) { user =>
    user.enabled ?? {
      logApi.closeAccount(mod, user.id) inject user.some
    }
  }

  def reopenAccount(mod: String, username: String): Funit = withUser(username) { user =>
    !user.enabled ?? {
      (UserRepo enable user.id) >> logApi.reopenAccount(mod, user.id)
    }
  }

  def setTitle(mod: String, username: String, title: Option[String]): Funit = withUser(username) { user =>
    UserRepo.setTitle(user.id, title) >>
      lightUserApi.invalidate(user.id) >>
      logApi.setTitle(mod, user.id, title)
  }

  def setEmail(mod: String, username: String, email: String): Funit = withUser(username) { user =>
    UserRepo.email(user.id, email) >>
      UserRepo.setEmailConfirmed(user.id) >>
      logApi.setEmail(mod, user.id)
  }

  def ipban(mod: String, ip: String): Funit =
    (firewall blockIp ip) >> logApi.ipban(mod, ip)

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op

}
