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
    dnsApi: DnsApi
) {

  // email was already regex-validated at this stage
  def validate(email: EmailAddress): Option[EmailAddress] =

    // start by lower casing the entire address
    email.value.toLowerCase
      // separate name from domain
      .split('@') match {

        // gmail addresses
        case Array(name, domain) if gmailDomains(domain) => name
        .replace(".", "") // remove all dots
        .takeWhile('+'!=) // skip everything after the first +
        .some.filter(_.nonEmpty) // make sure something remains
        .map(radix => EmailAddress(s"$radix@$domain")) // okay

        // disposable addresses
        case Array(_, domain) if disposable fromDomain domain => none

        // other valid addresses
        case Array(name, domain) if domain contains "." => EmailAddress(s"$name@$domain").some

        // invalid addresses
        case _ => none
      }

  def isValid(email: EmailAddress) = validate(email).isDefined

  /**
   * Returns true if an E-mail address is taken by another user.
   * @param email The E-mail address to be checked
   * @param forUser Optionally, the user the E-mail address field is to be assigned to.
   *                If they already have it assigned, returns false.
   * @return
   */
  private def isTakenBySomeoneElse(email: EmailAddress, forUser: Option[User]): Boolean =
    (lila.user.UserRepo.idByEmail(email) awaitSeconds 2, forUser) match {
      case (None, _) => false
      case (Some(userId), Some(user)) => userId != user.id
      case (_, _) => true
    }

  val acceptableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (isValid(EmailAddress(e))) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) = Constraint[String]("constraint.email_unique") { e =>
    if (isTakenBySomeoneElse(EmailAddress(e), forUser))
      Invalid(ValidationError("error.email_unique"))
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
  private def hasAcceptableDns(e: EmailAddress): Fu[Boolean] = validate(e) ?? {
    _.domain ?? { domain =>
      if (DisposableEmailDomain whitelisted domain) fuccess(true)
      else {
        dnsApi.a(domain) >>| {
          domain.value.startsWith("students.") ?? dnsApi.a(Domain(domain.value drop "students.".size))
        }
      } >>& dnsApi.mx(domain).map { domains =>
        domains.nonEmpty && domains.forall { !disposable(_) }
      }
    }
  }

  // the DNS emails should have been preloaded
  private[security] val withAcceptableDns = Constraint[String]("constraint.email_acceptable") { e =>
    val ok = hasAcceptableDns(EmailAddress(e)).awaitOrElse(100.millis, {
      logger.warn(s"EmailAddressValidator.withAcceptableDns timeout! ${e} records should have been preloaded")
      true
    })
    if (ok) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  private val gmailDomains = Set("gmail.com", "googlemail.com")
}
