package lila.security

import lila.user.{ User, Users, UserRepo }

import play.api.libs.concurrent.Execution.Implicits._

private[security] final class Cli(env: Env, userRepo: UserRepo) {

  import env._

  def enable(username: String) = perform(username, u ⇒ userRepo enable u.id)

  def disable(username: String) = perform(username, u ⇒
    (userRepo disable u.id) >> (store deleteUsername u.id)
  )

  def passwd(username: String, password: String) = perform(username, user ⇒
    userRepo.passwd(user.id, password)
  )

  def roles(username: String) = userRepo.find byId username map { 
    _.fold(s"User $username not found")(_.roles mkString " ")
  }

  def grant(username: String, roles: List[String]) = perform(username, user ⇒
    userRepo.setRoles(user.id, roles map (_.toUpperCase))
  )

  private def perform(username: String, op: User ⇒ Funit): Fu[String] = 
    userRepo.find byId Users.normalize(username) flatMap { userOption =>
      userOption.fold(fufail[String](s"User $username not found")) { u ⇒ 
        op(u) inject s"User $username successfully updated" 
      }
    } 
}
