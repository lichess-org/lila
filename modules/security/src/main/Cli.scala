package lila.security

import lila.user.{ User, UserRepo }
import lila.common.EmailAddress
import lila.common.Domain

final private[security] class Cli(userRepo: UserRepo, emailValidator: EmailAddressValidator)(using
    ec: Executor
) extends lila.common.Cli:

  def process =

    case "security" :: "roles" :: uid :: Nil =>
      userRepo byId UserStr(uid) dmap {
        _.fold("User %s not found" format uid)(_.roles mkString " ")
      }

    case "security" :: "grant" :: uid :: roles =>
      perform(UserStr(uid), user => userRepo.setRoles(user.id, roles map (_.toUpperCase)).void)

    case "disposable" :: "test" :: emailOrDomain :: Nil =>
      EmailAddress
        .from(emailOrDomain)
        .flatMap(_.domain)
        .orElse(Domain.from(emailOrDomain))
        .fold(fuccess("Invalid email or domain")) { dom =>
          emailValidator.validateDomain(dom.lower) map { r =>
            s"$r ${r.error | ""}"
          }
        }

  private def perform(u: UserStr, op: User => Funit): Fu[String] =
    userRepo byId u flatMap { userOption =>
      userOption.fold(fufail[String]("User %s not found" format u)) { u =>
        op(u) inject "User %s successfully updated".format(u)
      }
    }
