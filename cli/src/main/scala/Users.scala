package lila.cli

import lila.user.{ User, UserRepo }
import scalaz.effects._

case class Users(userRepo: UserRepo) {

  def enable(username: String): IO[Unit] =
    perform(username, "Enable", userRepo.enable)

  def disable(username: String): IO[Unit] =
    perform(username, "Disable", userRepo.disable)

  def perform(username: String, action: String, op: User ⇒ IO[Unit]) = for {
    _ ← putStrLn(action + " " + username)
    userOption ← userRepo byUsername username
    _ ← userOption.fold(
      u ⇒ op(u) flatMap { _ ⇒ putStrLn("Success") },
      putStrLn("Not found")
    )
  } yield ()
}
