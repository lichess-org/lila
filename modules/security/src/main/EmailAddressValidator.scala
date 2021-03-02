package lila.security

import play.api.data.validation._
import scala.concurrent.duration._

import lila.common.EmailAddress
import lila.user.{ User, UserRepo }

/** Validate and normalize emails
  */
final class EmailAddressValidator(
    userRepo: UserRepo,
    disposable: DisposableEmailDomain,
    dnsApi: DnsApi,
    checkMail: CheckMail
) {

  private def isAcceptable(email: EmailAddress): Boolean =
    email.domain exists disposable.isOk

  def validate(email: EmailAddress): Option[EmailAddressValidator.Acceptable] =
    isAcceptable(email) option EmailAddressValidator.Acceptable(email)

  /** Returns true if an E-mail address is taken by another user.
    * @param email The E-mail address to be checked
    * @param forUser Optionally, the user the E-mail address field is to be assigned to.
    *                If they already have it assigned, returns false.
    * @return
    */
  private def isTakenBySomeoneElse(email: EmailAddress, forUser: Option[User]): Fu[Boolean] =
    userRepo.idByEmail(email.normalize) dmap (_ -> forUser) dmap {
      case (None, _)                  => false
      case (Some(userId), Some(user)) => userId != user.id
      case (_, _)                     => true
    }

  private def wasUsedTwiceRecently(email: EmailAddress): Fu[Boolean] =
    userRepo.countRecentByPrevEmail(email.normalize).dmap(1 <)

  val acceptableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (EmailAddress.from(e).exists(isAcceptable)) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  val sendableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (EmailAddress.from(e).exists(_.isSendable)) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) =
    Constraint[String]("constraint.email_unique") { e =>
      val email = EmailAddress(e)
      val (taken, reused) =
        (isTakenBySomeoneElse(email, forUser) zip wasUsedTwiceRecently(email)).await(2 seconds, "emailUnique")
      if (taken || reused) Invalid(ValidationError("error.email_unique"))
      else Valid
    }

  def differentConstraint(than: Option[EmailAddress]) =
    Constraint[String]("constraint.email_different") { e =>
      if (than has EmailAddress(e))
        Invalid(ValidationError("error.email_different"))
      else Valid
    }

  // make sure the cache is warmed up, so next call can be synchronous
  def preloadDns(e: EmailAddress): Funit = hasAcceptableDns(e).void

  // only compute valid and non-whitelisted email domains
  private def hasAcceptableDns(e: EmailAddress): Fu[Boolean] =
    isAcceptable(e) ?? e.domain.map(_.lower) ?? { domain =>
      if (DisposableEmailDomain whitelisted domain) fuccess(true)
      else
        dnsApi.mx(domain).dmap { domains =>
          domains.nonEmpty && !domains.exists { disposable(_) }
        } >>& checkMail(domain)
    }

  // the DNS emails should have been preloaded
  private[security] val withAcceptableDns = Constraint[String]("constraint.email_acceptable") { e =>
    if (
      EmailAddress.from(e).exists { email =>
        hasAcceptableDns(email).awaitOrElse(
          90.millis,
          "dns", {
            logger.warn(
              s"EmailAddressValidator.withAcceptableDns timeout! $e records should have been preloaded"
            )
            false
          }
        )
      }
    ) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }
}

object EmailAddressValidator {
  case class Acceptable(acceptable: EmailAddress)
}
