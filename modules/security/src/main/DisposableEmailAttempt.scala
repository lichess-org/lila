package lila.security

import play.api.data.Form

import lila.common.EmailAddress
import lila.common.IpAddress
import lila.memo.CacheApi
import lila.user.User

final class DisposableEmailAttempt(
    cacheApi: CacheApi,
    disposableApi: DisposableEmailDomain,
    irc: lila.irc.IrcApi
):

  import DisposableEmailAttempt.*

  private val byIp =
    cacheApi.notLoadingSync[IpAddress, Set[Attempt]](64, "security.disposableEmailAttempt.ip") {
      _.expireAfterWrite(1 day).build()
    }

  private val byId =
    cacheApi.notLoadingSync[UserId, Set[Attempt]](64, "security.disposableEmailAttempt.id") {
      _.expireAfterWrite(1 day).build()
    }

  def onFail(form: Form[?], ip: IpAddress): Unit = for {
    email <- form("email").value flatMap EmailAddress.from
    if email.domain.exists(disposableApi.apply)
    str <- form("username").value
    u   <- UserStr read str
  } yield
    val attempt = Attempt(u.id, email, ip)
    byIp.underlying.asMap.compute(ip, (_, attempts) => ~Option(attempts) + attempt).unit
    byId.underlying.asMap.compute(u.id, (_, attempts) => ~Option(attempts) + attempt).unit

  def onSuccess(user: User, email: EmailAddress, ip: IpAddress) =
    val attempts = ~byIp.getIfPresent(ip) ++ ~byId.getIfPresent(user.id)
    if (
      attempts.sizeIs > 3 || (
        attempts.nonEmpty && email.domain.exists(d => !DisposableEmailDomain.whitelisted(d))
      )
    ) irc.signupAfterTryingDisposableEmail(user, email, attempts.map(_.email))

private object DisposableEmailAttempt:

  case class Attempt(id: UserId, email: EmailAddress, ip: IpAddress)
