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

  private def isTakenBy(email: String, forUser: Option[User]): Option[String] = validate(email) ?? { e =>
    (lila.user.UserRepo.idByEmail(e) awaitSeconds 2, forUser) match {
      case (None, _)                  => none
      case (Some(userId), Some(user)) => userId != user.id option userId
      case (someUserId, _)            => someUserId
    }
  }

  val acceptableConstraint = Constraint[String]("constraint.email_acceptable") { e =>
    if (isValid(e)) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) = Constraint[String]("constraint.email_unique") { e =>
    isTakenBy(e, forUser) match {
      case Some(userId) => Invalid(ValidationError(s"Email already in use by $userId"))
      case None         => Valid
    }
  }

  private val gmailDomains = Set("gmail.com", "googlemail.com")
}
