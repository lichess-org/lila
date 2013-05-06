package lila.mod

import lila.user.{ User, UserRepo, EloUpdater }
import lila.security.{ Firewall, UserSpy, Store ⇒ SecurityStore }
import lila.user.tube.userTube
import lila.db.api._
import lila.hub.actorApi.lobby.Censor

import akka.actor.ActorRef

final class ModApi(
    logApi: ModlogApi,
    userSpy: String ⇒ Fu[UserSpy],
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbySocket: ActorRef) {

  def adjust(mod: String, username: String): Funit = withUser(username) { user ⇒
    logApi.engine(mod, user.id, !user.engine) zip
      UserRepo.toggleEngine(user.id) zip
      eloUpdater.adjust(user)
  }

  def mute(mod: String, username: String): Funit = withUser(username) { user ⇒
    (UserRepo toggleMute user.id) >>-
    censor(user) >>
      logApi.mute(mod, user.id, !user.isChatBan)
  }

  def ban(mod: String, username: String): Funit = withUser(username) { user ⇒
    userSpy(username) flatMap { spy ⇒
      (spy.ips map firewall.blockIp).sequence >>-
      censor(user) >>
        logApi.ban(mod, user.id)
    }
  }

  def ipban(mod: String, ip: String): Funit =
    (firewall blockIp ip) >> logApi.ipban(mod, ip)

  private def censor(user: User) {
    // TODO handle that on lobby side
    if (user.canChat) lobbySocket ! Censor(user.username)
  }

  private def withUser(username: String)(op: User ⇒ Fu[Any]): Funit =
    $find.byId[User](username) flatMap { _ zmap (u ⇒ op(u).void) }
}
