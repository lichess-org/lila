package lila.security

import play.api.data.validation.*

import lila.common.{ Domain, EmailAddress }
import lila.user.{ User, UserRepo }

/** Validate and normalize emails
  */
final class EmailAddressValidator(
    userRepo: UserRepo,
    disposable: DisposableEmailDomain,
    dnsApi: DnsApi,
    checkMail: CheckMail
)(using Executor):

  import EmailAddressValidator.*

  val sendableConstraint = Constraint[EmailAddress]("constraint.email_acceptable") { email =>
    if (email.isSendable) Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) =
    Constraint[EmailAddress]("constraint.email_unique") { email =>
      val (taken, reused) =
        (isTakenBySomeoneElse(email, forUser) zip wasUsedTwiceRecently(email)).await(2 seconds, "emailUnique")
      if (taken || reused) Invalid(ValidationError("error.email_unique"))
      else Valid
    }

  def differentConstraint(than: Option[EmailAddress]) =
    Constraint[EmailAddress]("constraint.email_different") { email =>
      if (than has email) Invalid(ValidationError("error.email_different"))
      else Valid
    }

  // make sure the cache is warmed up, so next call can be synchronous
  def preloadDns(e: EmailAddress): Funit = apply(e).void

  // only compute valid and non-whitelisted email domains
  private[security] def apply(e: EmailAddress): Fu[Result] =
    e.domain.map(_.lower).fold(fuccess(Result.DomainMissing))(validateDomain)

  private[security] def validateDomain(domain: Domain.Lower): Fu[Result] =
    if DisposableEmailDomain.whitelisted(domain into Domain) then fuccess(Result.Passlist)
    else if disposable(domain into Domain) then fuccess(Result.Blocklist)
    else
      dnsApi.mx(domain).flatMap { domains =>
        if domains.isEmpty then fuccess(Result.DnsMissing)
        else if domains.exists(disposable.asMxRecord) then fuccess(Result.DnsBlocklist)
        else
          checkMail(domain).map { ck =>
            if ck then Result.Alright else Result.Reputation
          }
      }

  // the DNS emails should have been preloaded
  private[security] val withAcceptableDns = Constraint[EmailAddress]("constraint.email_acceptable") { email =>
    val result: Result = apply(email).awaitOrElse(
      90.millis,
      "dns", {
        logger.warn(
          s"EmailAddressValidator.withAcceptableDns timeout! $email records should have been preloaded"
        )
        Result.DnsTimeout
      }
    )
    result.error.fold(Valid)(e => Invalid(ValidationError(e)))
  }

  /** Returns true if an E-mail address is taken by another user.
    * @param email
    *   The E-mail address to be checked
    * @param forUser
    *   Optionally, the user the E-mail address field is to be assigned to. If they already have it assigned,
    *   returns false.
    * @return
    */
  private def isTakenBySomeoneElse(email: EmailAddress, forUser: Option[User]): Fu[Boolean] =
    userRepo.idByEmail(email.normalize) dmap (_ -> forUser) dmap {
      case (None, _)                  => false
      case (Some(userId), Some(user)) => userId != user.id
      case (_, _)                     => true
    }

  private def wasUsedTwiceRecently(email: EmailAddress): Fu[Boolean] =
    userRepo.countRecentByPrevEmail(email.normalize, nowInstant.minusWeeks(1)).dmap(_ >= 2) >>|
      userRepo.countRecentByPrevEmail(email.normalize, nowInstant.minusMonths(1)).dmap(_ >= 4)

object EmailAddressValidator:
  enum Result(val error: Option[String]):
    def valid = error.isEmpty
    case Passlist      extends Result(none)
    case Alright       extends Result(none)
    case DomainMissing extends Result("The email address domain is missing.".some) // no translation needed
    case Blocklist     extends Result("Cannot use disposable email addresses (Blocklist).".some)
    case DnsMissing    extends Result("This email domain doesn't seem to work (missing MX DNS)".some)
    case DnsTimeout    extends Result("This email domain doesn't seem to work (timeout MX DNS)".some)
    case DnsBlocklist
        extends Result(
          "Cannot use disposable email addresses (DNS blocklist).".some
        )
    case Reputation extends Result("This email domain has a poor reputation and cannot be used.".some)
