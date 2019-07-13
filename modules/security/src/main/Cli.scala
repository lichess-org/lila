package lila.security

import lila.user.{ User, UserRepo }

private[security] final class Cli extends lila.common.Cli {

  def process = {

    case "security" :: "roles" :: sri :: Nil =>
      UserRepo named sri map {
        _.fold("User %s not found" format sri)(_.roles mkString " ")
      }

    case "security" :: "grant" :: sri :: roles =>
      perform(sri, user =>
        UserRepo.setRoles(user.id, roles map (_.toUpperCase)).void)
  }

  private def perform(username: String, op: User => Funit): Fu[String] =
    UserRepo named username flatMap { userOption =>
      userOption.fold(fufail[String]("User %s not found" format username)) { u =>
        op(u) inject "User %s successfully updated".format(username)
      }
    }
}
