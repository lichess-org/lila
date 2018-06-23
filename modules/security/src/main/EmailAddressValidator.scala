package lila.security

import lila.user.User
import lila.common.EmailAddress

import play.api.data.validation._

/**
 * Validate and normalize emails
 */
final class EmailAddressValidator(disposable: DisposableEmailDomain) {

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
        case Array(_, domain) if disposable(domain) => none

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
  private def isTakenBySomeoneElse(email: EmailAddress, forUser: Option[User]): Option[String] = validate(email) ?? { e =>
    (lila.user.UserRepo.idByEmail(e) awaitSeconds 2, forUser) match {
      case (None, _) => none
      case (Some(userId), Some(user)) => {
        if (userId != user.id) isSameEmail(e, email)
        else none
      }
      case (_, _) => isSameEmail(e, email)
    }
  }

  private def isSameEmail(email1: EmailAddress, email2: EmailAddress): Option[String] =
    if (email1.value.toLowerCase == email2.value.toLowerCase) "error.email_unique".some
    else "error.email_indistinct".some

  val acceptableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (isValid(EmailAddress(e))) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) = Constraint[String]("constraint.email_unique") { e =>
    isTakenBySomeoneElse(EmailAddress(e), forUser) match {
      case None => Valid
      case Some(err) => Invalid(ValidationError(err))
    }
  }

  def differentConstraint(than: Option[EmailAddress]) = Constraint[String]("constraint.email_different") { e =>
    if (than has EmailAddress(e))
      Invalid(ValidationError("error.email_different"))
    else Valid
  }

  private val gmailDomains = Set("gmail.com", "googlemail.com")
}
