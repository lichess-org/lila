package lila
package mod

import user.{ User, UserRepo }
import elo.EloUpdater
import lobby.Messenger
import security.{ Firewall, UserSpy }

import scalaz.effects._

final class ModApi(
    logApi: ModlogApi,
    userRepo: UserRepo,
    userSpy: String ⇒ IO[UserSpy],
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

  def ban(mod: User, username: String): IO[Unit] = withUser(username) { user ⇒
    userSpy(username) flatMap { spy ⇒
      io(spy.ips foreach firewall.blockIp) >>
        (lobbyMessenger mute user.username doUnless user.isChatBan) >>
        logApi.ban(mod, user)
    }
  }

  def ipban(mod: User, ip: String): IO[Unit] = for {
    _ ← io(firewall blockIp ip)
    _ ← logApi.ipban(mod, ip)
  } yield ()

  private def withUser(username: String)(userIo: User ⇒ IO[Unit]) = for {
    userOption ← userRepo byId username
    _ ← userOption.fold(userIo, io())
  } yield ()
}
