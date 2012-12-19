package lila
package cli

import lila.user.{ User, UserRepo }
import lila.security.Store
import scalaz.effects._

private[cli] case class Users(userRepo: UserRepo, securityStore: Store) {

  def enable(username: String): IO[Unit] =
    perform(username, userRepo.enable)

  def disable(username: String): IO[Unit] =
    perform(username, user ⇒
      userRepo disable user map { _ ⇒
        securityStore deleteUsername username
      }
    )

  def passwd(username: String, password: String) =
    perform(username, user ⇒ {
      userRepo.passwd(user, password) map (_.fold(
        errors ⇒ throw new RuntimeException(errors.shows),
        _ ⇒ io()
      ))
    })

  def track(username: String) = io {
    println("%s = %s".format(
      username, 
      securityStore explore username mkString ", "
    ))
  }

  private def perform(username: String, op: User ⇒ IO[Unit]) = for {
    userOption ← userRepo byId username
    _ ← userOption.fold(
      u ⇒ op(u) flatMap { _ ⇒ putStrLn("Success") },
      putStrLn("Not found")
    )
  } yield ()
}
