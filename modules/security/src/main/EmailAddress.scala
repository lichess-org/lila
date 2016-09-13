package lila.security

import lila.user.User

import play.api.data.validation._

/**
 * Validate and normalize emails
 */
final class EmailAddress(disposable: DisposableEmailDomain) {

  // email was already regex-validated at this stage
  def validate(email: String): Option[String] =

    // start by lower casing the entire address
    email.toLowerCase
      // separate name from domain
      .split('@') match {

        // gmail addresses
        case Array(name, domain) if gmailDomains(domain) => name
        .replace(".", "") // remove all dots
        .takeWhile('+'!=) // skip everything after the first +
        .some.filter(_.nonEmpty) // make sure something remains
        .map(radix => s"$radix@$domain") // okay

        // disposable addresses
        case Array(_, domain) if disposable(domain) => none

        // other valid addresses
        case Array(name, domain)                    => s"$name@$domain".some

        // invalid addresses for match exhaustivity sake
        case _                                      => none
      }

  def isValid(email: String) = validate(email).isDefined

  /**
    * Returns true if an E-mail address is taken by another user.
    * @param email The E-mail address to be checked
    * @param forUser Optionally, the user the E-mail address field is to be assigned to.
    *                If they already have it assigned, returns false.
    * @return
    */
  private def isTakenBySomeoneElse(email: String, forUser: Option[User]): Boolean = validate(email) ?? { e =>
    (lila.user.UserRepo.idByEmail(e) awaitSeconds 2, forUser) match {
      case (None, _)                  => false
      case (Some(userId), Some(user)) => userId != user.id
      case (_, _)            => true
    }
  }

  val acceptableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (isValid(e)) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) = Constraint[String]("constraint.email_unique") { e =>
    if (isTakenBySomeoneElse(e, forUser))
      Invalid(ValidationError(s"Email address is already in use by another account"))
    else Valid
  }

  private val gmailDomains = Set("gmail.com", "googlemail.com")
}
