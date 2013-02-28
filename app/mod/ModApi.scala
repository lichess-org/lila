package lila.app
package mod

import user.{ User, UserRepo }
import elo.EloUpdater
import lobby.Messenger
import security.{ Firewall, UserSpy, Store ⇒ SecurityStore }
import lila.common.Futuristic._

import scalaz.effects._
import play.api.libs.concurrent.Execution.Implicits._

final class ModApi(
    logApi: ModlogApi,
    userRepo: UserRepo,
    userSpy: String ⇒ IO[UserSpy],
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbyMessenger: Messenger) {

  def adjust(mod: User, username: String): Funit = withUser(username) { user ⇒
    logApi.engine(mod, user, !user.engine) zip
    userRepo.toggleEngine(user.id) zip
    eloUpdater.adjust(user) map toVoid
  }

  def mute(mod: User, username: String): Funit = for {
    userOption ← userRepo byId username
    _ ← ~userOption.map(user ⇒ for {
      _ ← userRepo toggleMute user.id
      _ ← lobbyMessenger mute user.username doUnless user.isChatBan
      _ ← logApi.mute(mod, user, !user.isChatBan)
    } yield ())
  } yield ()

  def ban(mod: User, username: String): Funit = withUser(username) { user ⇒
    userSpy(username) flatMap { spy ⇒
      io(spy.ips foreach firewall.blockIp) >>
        (lobbyMessenger mute user.username doUnless user.isChatBan) >>
        logApi.ban(mod, user)
    }
  }

  def ipban(mod: User, ip: String): Funit = for {
    _ ← io(firewall blockIp ip)
    _ ← logApi.ipban(mod, ip)
  } yield ()

  private def withUser(username: String)(op: User ⇒ Funit): Funit = for {
    userOption ← userRepo byId username
    _ ← ~userOption.map(op)
  } yield ()
}
