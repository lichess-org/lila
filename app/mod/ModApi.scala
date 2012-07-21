package lila
package mod

import user.{ User, UserRepo }
import elo.EloUpdater
import lobby.Messenger
import security.{ Firewall, Store => SecurityStore }

import scalaz.effects._

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
    _ ← userOption.fold(
      user ⇒ for {
        _ ← userRepo toggleMute user.id
        _ ← lobbyMessenger mute user.username doUnless user.isChatBan
        _ ← logApi.mute(mod, user, !user.isChatBan)
      } yield (),
      io())
  } yield ()

  def ipban(mod: User, username: String): IO[Unit] = withUser(username) { user ⇒
    for {
      spy ← securityStore userSpy username
      _ ← io(spy.ips foreach firewall.blockIp)
      _ ← lobbyMessenger mute user.username doUnless user.isChatBan
      _ ← logApi.ipban(mod, user)
    } yield ()
  }

  private def withUser(username: String)(userIo: User ⇒ IO[Unit]) = for {
    userOption ← userRepo byId username
    _ ← userOption.fold(userIo, io())
  } yield ()
}
