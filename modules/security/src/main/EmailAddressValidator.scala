package lila.security

import play.api.data.validation.*
import scala.concurrent.duration.*

import lila.common.{ Domain, EmailAddress }
import lila.user.{ User, UserRepo }
import scala.concurrent.ExecutionContext
import org.joda.time.DateTime

/** Validate and normalize emails
  */
final class EmailAddressValidator(
    userRepo: UserRepo,
    disposable: DisposableEmailDomain,
    dnsApi: DnsApi,
    checkMail: CheckMail
)(using ExecutionContext):

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
  private[security] def apply(e: EmailAddress): Fu[Boolean] = e.domain.map(_.lower) ?? validateDomain

  private[security] def validateDomain(domain: Domain.Lower): Fu[Boolean] =
    disposable.isOk(domain into Domain) ?? {
      if (DisposableEmailDomain whitelisted domain) fuccess(true)
      else
        dnsApi.mx(domain).dmap { domains =>
          domains.nonEmpty && !domains.exists { disposable(_) }
        } >>& checkMail(domain)
    }

  // the DNS emails should have been preloaded
  private[security] val withAcceptableDns = Constraint[EmailAddress]("constraint.email_acceptable") { email =>
    if (
      apply(email).awaitOrElse(
        90.millis,
        "dns", {
          logger.warn(
            s"EmailAddressValidator.withAcceptableDns timeout! $email records should have been preloaded"
          )
          false
        }
      )
    ) Valid
    else Invalid(ValidationError("error.email_acceptable"))
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
    userRepo.countRecentByPrevEmail(email.normalize, DateTime.now.minusWeeks(1)).dmap(_ >= 2) >>|
      userRepo.countRecentByPrevEmail(email.normalize, DateTime.now.minusMonths(1)).dmap(_ >= 4)

object EmailAddressValidator:
