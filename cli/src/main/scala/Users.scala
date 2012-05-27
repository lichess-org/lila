package lila.cli

import lila.user.{ User, UserRepo }
import lila.security.Store
import scalaz.effects._

case class Users(userRepo: UserRepo, securityStore: Store) {

  def enable(username: String): IO[Unit] =
    perform(username, "Enable", userRepo.enable)

  def disable(username: String): IO[Unit] =
    perform(username, "Disable", user ⇒
      userRepo disable user map { _ ⇒
        securityStore deleteUsername username
      }
    )

  def perform(username: String, action: String, op: User ⇒ IO[Unit]) = for {
    _ ← putStrLn(action + " " + username)
    userOption ← userRepo byUsername username
    _ ← userOption.fold(
      u ⇒ op(u) flatMap { _ ⇒ putStrLn("Success") },
      putStrLn("Not found")
    )
  } yield ()

  def info(username: String) =
    securityStore.userInfo(username).fold(
      info ⇒ for {
        _ ← putStrLn("USER " + info.user)
        _ ← putStrLn("IP   " + info.ip)
        _ ← putStrLn("UA   " + info.ua)
        _ ← putStrLn("DATE " + info.date)
      } yield (),
      putStrLn("Not found")
    )
}
