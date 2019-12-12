package lila.security

import play.api.data.validation._
import scala.concurrent.duration._

import lila.common.{ EmailAddress, Domain }
import lila.user.User

/**
 * Validate and normalize emails
 */
final class EmailAddressValidator(
    disposable: DisposableEmailDomain,
    dnsApi: DnsApi,
    checkMail: CheckMail
) {

  private def isAcceptable(email: EmailAddress): Boolean =
    email.domain exists disposable.isOk

  def validate(email: EmailAddress): Option[EmailAddressValidator.Acceptable] =
    isAcceptable(email) option EmailAddressValidator.Acceptable(email)

  /**
   * Returns true if an E-mail address is taken by another user.
   * @param email The E-mail address to be checked
   * @param forUser Optionally, the user the E-mail address field is to be assigned to.
   *                If they already have it assigned, returns false.
   * @return
   */
  private def isTakenBySomeoneElse(email: EmailAddress, forUser: Option[User]): Fu[Boolean] =
    lila.user.UserRepo.idByEmail(email.normalize) map (_ -> forUser) map {
      case (None, _) => false
      case (Some(userId), Some(user)) => userId != user.id
      case (_, _) => true
    }

  private def wasUsedTwiceRecently(email: EmailAddress): Fu[Boolean] =
    lila.user.UserRepo.countRecentByPrevEmail(email.normalize).map(1<)

  val acceptableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (isAcceptable(EmailAddress(e))) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) = Constraint[String]("constraint.email_unique") { e =>
    val email = EmailAddress(e)
    val (taken, reused) = (isTakenBySomeoneElse(email, forUser) zip wasUsedTwiceRecently(email)) awaitSeconds 2
    if (taken || reused) Invalid(ValidationError("error.email_unique"))
    else Valid
  }

  def differentConstraint(than: Option[EmailAddress]) = Constraint[String]("constraint.email_different") { e =>
    if (than has EmailAddress(e))
      Invalid(ValidationError("error.email_different"))
    else Valid
  }

  // make sure the cache is warmed up, so next call can be synchronous
  def preloadDns(e: EmailAddress): Funit = hasAcceptableDns(e).void

  // only compute valid and non-whitelisted email domains
  private def hasAcceptableDns(e: EmailAddress): Fu[Boolean] =
    if (isAcceptable(e)) e.domain.map(_.lower) ?? { domain =>
      if (DisposableEmailDomain whitelisted domain) fuccess(true)
      else dnsApi.mx(domain).map { domains =>
        domains.nonEmpty && !domains.exists { disposable(_) }
      } >>& checkMail(domain)
    }
    else fuccess(false)

  // the DNS emails should have been preloaded
  private[security] val withAcceptableDns = Constraint[String]("constraint.email_acceptable") { e =>
    val ok = hasAcceptableDns(EmailAddress(e)).awaitOrElse(100.millis, {
      logger.warn(s"EmailAddressValidator.withAcceptableDns timeout! ${e} records should have been preloaded")
      true
    })
    if (ok) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }
}

object EmailAddressValidator {
  case class Acceptable(acceptable: EmailAddress)
}
