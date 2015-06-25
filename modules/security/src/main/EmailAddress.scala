package lila.security

import play.api.data.validation._

/**
 * Validate and normalize emails
 */
private final class EmailAddress(disposable: DisposableEmail) {

  // email was already regex-validated at this stage
  def validate(email: String): Option[String] =

    // start by lower casing the entire address
    email.toLowerCase
      // separate name from domain
      .split('@') match {

        // gmail addresses
        case Array(name, domain) if isGmail(domain) => name
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

  val constraint = Constraint[String]("constraint.email") { e =>
    if (isValid(e)) Valid
    else Invalid(ValidationError("error.email"))
  }

  private def isGmail(domain: String) = domain == "gmail.com"
}
