package lila
package mod

import user.{ User, UserRepo }
import elo.EloUpdater
import lobby.Messenger
import security.{ Firewall, Store ⇒ SecurityStore }
import core.Futuristic._

import scalaz.effects._
import play.api.libs.concurrent.Execution.Implicits._

final class ModApi(
    logApi: ModlogApi,
    userRepo: UserRepo,
    securityStore: SecurityStore,
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbyMessenger: Messenger) {

  def adjust(mod: User, username: String): IO[Unit] = withUser(username) { user ⇒
    for {
      _ ← userRepo toggleEngine user.id
      _ ← eloUpdater adjust user
      _ ← logApi.engine(mod, user, !user.engine)
    } yield ()
  }

  def mute(mod: User, username: String): IO[Unit] = for {
    userOption ← userRepo byId username
    _ ← ~userOption.map(user ⇒ for {
      _ ← userRepo toggleMute user.id
      _ ← lobbyMessenger mute user.username doUnless user.isChatBan
      _ ← logApi.mute(mod, user, !user.isChatBan)
    } yield ())
  } yield ()

  def ban(mod: User, username: String): IO[Unit] = withUser(username) { user ⇒
    for {
      spy ← securityStore userSpy username
      _ ← io(spy.ips foreach firewall.blockIp)
      _ ← lobbyMessenger mute user.username doUnless user.isChatBan
      _ ← logApi.ban(mod, user)
    } yield ()
  }

  def ipban(mod: User, ip: String): IO[Unit] = for {
    _ ← io(firewall blockIp ip)
    _ ← logApi.ipban(mod, ip)
  } yield ()

  private def withUser(username: String)(userIo: User ⇒ IO[Unit]) = for {
    userOption ← userRepo byId username
    _ ← ~userOption.map(userIo)
  } yield ()
}
