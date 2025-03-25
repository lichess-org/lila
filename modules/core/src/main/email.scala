package lila.core

import scalalib.newtypes.OpaqueString

import lila.core.net.Domain

object email:

  opaque type EmailAddress = String
  object EmailAddress extends OpaqueString[EmailAddress]:

    extension (e: EmailAddress)

      def username = e.takeWhile(_ != '@')

      def conceal = e.split('@') match
        case Array(name, domain) => s"${name.take(3)}*****@$domain"
        case _                   => e

      def normalize = NormalizedEmailAddress: // changing normalization requires database migration!
        val lower = e.toLowerCase
        lower.split('@') match
          case Array(name, domain) =>
            val skipAfterPlus = name.takeWhile('+' != _)
            val normalizedName =
              if EmailAddress.gmailLikeNormalizedDomains(domain) then skipAfterPlus.replace(".", "")
              else skipAfterPlus
            if normalizedName.isEmpty then lower else s"$normalizedName@$domain"
          case _ => lower

      def domain: Option[Domain] =
        e.split('@') match
          case Array(_, domain) => Domain.from(domain.toLowerCase)
          case _                => none

      def nameAndDomain: Option[(String, Domain)] = domain.map: d =>
        e.takeWhile(_ != '@') -> d

      def similarTo(other: EmailAddress) =
        e.normalize.eliminateDomainAlias == other.normalize.eliminateDomainAlias

      def isNoReply  = e.startsWith("noreply.") && e.endsWith("@lichess.org")
      def isBlank    = e.startsWith("noreply.blanked.")
      def isSendable = !e.isNoReply && !e.isBlank

      def looksLikeFakeEmail =
        e.domain.map(_.lower).exists(EmailAddress.gmailDomains.contains) &&
          e.username.count('.' == _) >= 4

      def eliminateDomainAlias: EmailAddress =
        e.nameAndDomain.fold(e): (name, domain) =>
          val newDomain =
            if yandexDomains.contains(domain.lower) then "yandex.com"
            else if gmailDomains.contains(domain.lower) then "gmail.com"
            else domain
          s"$name@$newDomain"

    private val regex =
      """(?i)^[a-z0-9.!#$%&'*+/=?^_`{|}~\-]+@[a-z0-9](?:[a-z0-9-]{0,62}+(?<!-))?(?:\.[a-z0-9](?:[a-z0-9-]{0,62}+(?<!-))?)*$""".r

    val maxLength = 320

    val gmailDomains: Set[Domain.Lower] = Domain.Lower.from(Set("gmail.com", "googlemail.com"))
    val yandexDomains: Set[Domain.Lower] =
      Domain.Lower.from(Set("yandex.com", "yandex.ru", "ya.ru", "yandex.ua", "yandex.kz", "yandex.by"))

    // adding normalized domains requires database migration!
    private val gmailLikeNormalizedDomains =
      gmailDomains ++ Set("protonmail.com", "protonmail.ch", "pm.me", "proton.me")

    def isValid(str: String) =
      str.sizeIs < maxLength &&
        regex.matches(str) && !str.contains("..") && !str.contains(".@") && !str.startsWith(".")

    def from(str: String): Option[EmailAddress] = isValid(str).option(EmailAddress(str))

    val clasIdRegex = """^noreply\.class\.(\w{8})\.[\w-]+@lichess\.org""".r

  opaque type NormalizedEmailAddress = String
  object NormalizedEmailAddress extends OpaqueString[NormalizedEmailAddress]

  opaque type UserStrOrEmail = String
  object UserStrOrEmail extends OpaqueString[UserStrOrEmail]:
    extension (e: UserStrOrEmail)
      def normalize: UserIdOrEmail =
        EmailAddress.from(e).fold(e.toLowerCase)(e => EmailAddress.normalize(e).value)

  opaque type UserIdOrEmail = String
  object UserIdOrEmail extends OpaqueString[UserIdOrEmail]
