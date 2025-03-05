package lila.security

import play.api.data.validation.*

import lila.core.net.Domain
import lila.user.{ User, UserRepo }
import lila.core.email.NormalizedEmailAddress

/** Validate and normalize emails
  */
final class EmailAddressValidator(
    userRepo: UserRepo,
    disposable: DisposableEmailDomain,
    dnsApi: DnsApi,
    verifyMail: VerifyMail
)(using Executor):

  import EmailAddressValidator.*

  val sendableConstraint = Constraint[EmailAddress]("constraint.email_acceptable") { email =>
    if email.isSendable then Valid
    else Invalid(ValidationError("error.email_acceptable"))
  }

  def uniqueConstraint(forUser: Option[User]) =
    Constraint[EmailAddress]("constraint.email_unique") { email =>
      val (taken, reused) =
        (isTakenBySomeoneElse(email, forUser)
          .zip(wasUsedTwiceRecently(email)))
          .await(2.seconds, "emailUnique")
      if taken || reused then Invalid(ValidationError("error.email_unique"))
      else Valid
    }

  def differentConstraint(than: Option[EmailAddress]) =
    Constraint[EmailAddress]("constraint.email_different") { email =>
      if than.has(email) then Invalid(ValidationError("error.email_different"))
      else Valid
    }

  // make sure the cache is warmed up, so next call can be synchronous
  def preloadDns(e: EmailAddress): Funit = apply(e).void

  // only compute valid and non-whitelisted email domains
  private[security] def apply(e: EmailAddress): Fu[Result] =
    if isInfiniteAlias(e) then fuccess(Result.Alias)
    else e.domain.map(_.lower).fold(fuccess(Result.DomainMissing))(validateDomain)

  private[security] def validateDomain(domain: Domain.Lower): Fu[Result] =
    if DisposableEmailDomain.whitelisted(domain.into(Domain)) then fuccess(Result.Passlist)
    else if disposable(domain.into(Domain)) then fuccess(Result.Blocklist)
    else
      dnsApi
        .mx(domain)
        .flatMap: domains =>
          if domains.isEmpty then fuccess(Result.DnsMissing)
          else if domains.exists(disposable.asMxRecord) then fuccess(Result.DnsBlocklist)
          else
            verifyMail(domain).map: ok =>
              if ok then Result.Alright else Result.Reputation

  // the DNS emails should have been preloaded
  private[security] val withAcceptableDns = Constraint[EmailAddress]("constraint.email_acceptable") { email =>
    val result: Result = apply(email).awaitOrElse(
      90.millis,
      "dns", {
        logger.warn:
          s"EmailAddressValidator.withAcceptableDns timeout! $email records should have been preloaded"
        Result.DnsTimeout
      }
    )
    result.error.fold(Valid)(e => Invalid(ValidationError(e)))
  }

  /* @param forUser
   *   Optionally, the user the E-mail address field is to be assigned to. If they already have it assigned,
   *   returns false.
   */
  private def isTakenBySomeoneElse(email: EmailAddress, forUser: Option[User]): Fu[Boolean] =
    val variations = domainAliasVariationsOf(email.normalize)
    userRepo
      .idByAnyEmail(variations)
      .dmap(_ -> forUser)
      .dmap:
        case (None, _)                  => false
        case (Some(userId), Some(user)) => user.isnt(userId)
        case (_, _)                     => true

  private def domainAliasVariationsOf(email: NormalizedEmailAddress): List[NormalizedEmailAddress] =
    val variations = email
      .into(EmailAddress)
      .nameAndDomain
      .so: (name, domain) =>
        List(EmailAddress.gmailDomains, EmailAddress.yandexDomains)
          .foldLeft(List.empty[NormalizedEmailAddress]):
            case (Nil, aliases) if aliases.contains(domain.lower) =>
              aliases.toList.map(d => EmailAddress(s"$name@$d").normalize)
            case (acc, _) => acc
    if variations.isEmpty then List(email) else variations

  private def isInfiniteAlias(e: EmailAddress) =
    duckAliases.is(e)

  private object duckAliases:
    private val domain      = Domain.Lower.from("duck.com")
    private val regex       = """^\w{3,}-\w{3,}-\w{3,}$""".r
    def is(e: EmailAddress) = e.nameAndDomain.exists((n, d) => d.lower == domain && regex.matches(n))

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
    case Alias         extends Result("Cannot use email address aliases.".some)
    case DnsMissing    extends Result("This email domain doesn't seem to work (missing MX DNS)".some)
    case DnsTimeout    extends Result("This email domain doesn't seem to work (timeout MX DNS)".some)
    case DnsBlocklist  extends Result("Cannot use disposable email addresses (DNS blocklist).".some)
    case Reputation    extends Result("This email domain has a poor reputation and cannot be used.".some)
