package lila.security

import lila.user.{ User, UserRepo }
import lila.common.EmailAddress
import lila.common.Domain

final private[security] class Cli(userRepo: UserRepo, disposable: DisposableEmailDomain)(implicit
    ec: scala.concurrent.ExecutionContext
) extends lila.common.Cli {

  def process = {

    case "security" :: "roles" :: uid :: Nil =>
      userRepo named uid dmap {
        _.fold("User %s not found" format uid)(_.roles mkString " ")
      }

    case "security" :: "grant" :: uid :: roles =>
      perform(uid, user => userRepo.setRoles(user.id, roles map (_.toUpperCase)).void)

    case "disposable" :: "test" :: emailOrDomain :: Nil =>
      fuccess {
        EmailAddress
          .from(emailOrDomain)
          .flatMap(_.domain)
          .orElse(Domain.from(emailOrDomain))
          .fold("Invalid email or domain") { dom =>
            if (disposable(dom)) "Blocked disposable domain"
            else "Accepted domain"
          }
      }
  }

  private def perform(username: String, op: User => Funit): Fu[String] =
    userRepo named username flatMap { userOption =>
      userOption.fold(fufail[String]("User %s not found" format username)) { u =>
        op(u) inject "User %s successfully updated".format(username)
      }
    }
}
