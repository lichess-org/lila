package lila.mod

import lila.user.{ User, UserRepo, EloUpdater }
import lila.security.{ Firewall, UserSpy, Store ⇒ SecurityStore }
import lila.user.tube.userTube
import lila.db.api._
import lila.hub.actorApi.lobby.Censor

final class ModApi(
    logApi: ModlogApi,
    userSpy: String ⇒ Fu[UserSpy],
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbySocket: lila.hub.ActorLazyRef) {

  def adjust(mod: String, userId: String): Funit = withUser(userId) { user ⇒
    logApi.engine(mod, user.id, !user.engine) zip
      UserRepo.toggleEngine(user.id) zip
      eloUpdater.adjust(user)
  }

  def troll(mod: String, userId: String): Funit = withUser(userId) { user ⇒
    (UserRepo toggleTroll user.id) >>-
      logApi.troll(mod, user.id, user.noTroll)
  }

  def ban(mod: String, userId: String): Funit = withUser(userId) { user ⇒
    userSpy(user.id) flatMap { spy ⇒
      UserRepo.toggleIpBan(user.id) >>
        logApi.ban(mod, user.id, !user.ipBan) >>
        user.ipBan.fold(
          (spy.ipStrings map firewall.unblockIp).sequence,
          (spy.ipStrings map firewall.blockIp).sequence >>
            (SecurityStore disconnect user.id) 
        )
    }
  }

  def ipban(mod: String, ip: String): Funit =
    (firewall blockIp ip) >> logApi.ipban(mod, ip)

  private def censor(user: User) {
    // TODO handle that on lobby side (or remove this message)
    if (user.troll) lobbySocket ! Censor(user.username)
  }

  private def withUser(userId: String)(op: User ⇒ Fu[Any]): Funit =
    UserRepo named userId flatMap { _ zmap (u ⇒ op(u).void) }
}
