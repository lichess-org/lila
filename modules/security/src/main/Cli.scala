package lila.security

import lila.user.{ User, UserRepo }

private[security] final class Cli extends lila.common.Cli {

  def process = {

    case "security" :: "passwd" :: uid :: pwd :: Nil =>
      perform(uid, user => UserRepo.passwd(user.id, pwd))

    case "security" :: "roles" :: uid :: Nil =>
      UserRepo named uid map {
        _.fold("User %s not found" format uid)(_.roles mkString " ")
      }

    case "security" :: "grant" :: uid :: roles =>
      perform(uid, user =>
        UserRepo.setRoles(user.id, roles map (_.toUpperCase)).void)
  }

  private def perform(username: String, op: User => Funit): Fu[String] =
    UserRepo named username flatMap { userOption =>
      userOption.fold(fufail[String]("User %s not found" format username)) { u =>
        op(u) inject "User %s successfully updated".format(username)
      }
    }
}
