package lila.security

import scalalib.net.Domain

export lila.core.lilaism.Lilaism.{ *, given }
import lila.core.email.{ EmailAddress, NormalizedEmailAddress }
export lila.common.extensions.*

private val logger = lila.log("security")

extension (e: EmailAddress)

  def looksLikeFakeEmail =
    e.domain.map(_.lower).exists(EmailAddress.gmailDomains.contains) && {
      val dots = e.username.count('.' == _)
      dots >= 3 || (dots == 2 && """\d\.\d""".r.unanchored.matches(e.username))
    }

  def similarTo(other: EmailAddress) =
    e.normalize.eliminateDomainAlias == other.normalize.eliminateDomainAlias

extension (e: NormalizedEmailAddress)

  def eliminateDomainAlias: NormalizedEmailAddress =
    e.into(EmailAddress)
      .nameAndDomain
      .fold(e): (name, domain) =>
        val newDomain =
          if yandexDomains.contains(domain.lower) then "yandex.com"
          else if EmailAddress.gmailDomains.contains(domain.lower) then "gmail.com"
          else domain
        NormalizedEmailAddress(s"$name@$newDomain")

private val yandexDomains: Set[Domain.Lower] =
  Domain.Lower.from(Set("yandex.com", "yandex.ru", "ya.ru", "yandex.ua", "yandex.kz", "yandex.by"))
