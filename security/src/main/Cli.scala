package lila.security

import lila.user.{ User, Users, UserRepo }

import play.api.libs.concurrent.Execution.Implicits._

private[security] final class Cli {

  def enable(username: String) = perform(username, u ⇒ UserRepo enable u.id)

  def disable(username: String) = perform(username, u ⇒
    (UserRepo disable u.id) >> (Store deleteUsername u.id)
  )

  def passwd(username: String, password: String) = perform(username, user ⇒
    UserRepo.passwd(user.id, password)
  )

  def roles(username: String) = UserRepo named username map { 
    _.fold(s"User $username not found")(_.roles mkString " ")
  }

  def grant(username: String, roles: List[String]) = perform(username, user ⇒
    UserRepo.setRoles(user.id, roles map (_.toUpperCase))
  )

  private def perform(username: String, op: User ⇒ Funit): Fu[String] = 
    UserRepo named username flatMap { userOption =>
      userOption.fold(fufail[String](s"User $username not found")) { u ⇒ 
        op(u) inject s"User $username successfully updated" 
      }
    } 
}
