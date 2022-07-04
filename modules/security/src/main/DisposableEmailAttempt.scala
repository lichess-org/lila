package lila.security

import play.api.data.Form
import scala.concurrent.duration._

import lila.common.EmailAddress
import lila.common.IpAddress
import lila.memo.CacheApi
import lila.user.User

final class DisposableEmailAttempt(
    cacheApi: CacheApi,
    disposableApi: DisposableEmailDomain,
    irc: lila.irc.IrcApi
) {

  import DisposableEmailAttempt._

  private val byIp =
    cacheApi.notLoadingSync[IpAddress, Set[Attempt]](64, "security.disposableEmailAttempt.ip") {
      _.expireAfterWrite(1 day).build()
    }

  private val byId =
    cacheApi.notLoadingSync[User.ID, Set[Attempt]](64, "security.disposableEmailAttempt.ip") {
      _.expireAfterWrite(1 day).build()
    }

  def onFail(form: Form[_], ip: IpAddress): Unit = for {
    email    <- form("email").value flatMap EmailAddress.from
    username <- form("username").value
    if email.domain.exists(disposableApi.apply)
  } {
    val id      = User normalize username
    val attempt = Attempt(id, email, ip)
    byId.underlying.asMap.compute(id, (_, attempts) => ~Option(attempts) + attempt).unit
  }

  def onSuccess(user: User, email: EmailAddress, ip: IpAddress) = {
    val attempts = ~byIp.getIfPresent(ip) ++ ~byId.getIfPresent(user.id)
    if (
      attempts.sizeIs > 3 || (
        attempts.nonEmpty && email.domain.exists(d => !DisposableEmailDomain.whitelisted(d.lower))
      )
    ) irc.signupAfterTryingDisposableEmail(user, email, attempts.map(_.email))
  }
}

private object DisposableEmailAttempt {

  case class Attempt(id: User.ID, email: EmailAddress, ip: IpAddress)
}
