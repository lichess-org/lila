package lila.security

import play.api.data.Form

import lila.core.net.IpAddress
import lila.memo.CacheApi

final class DisposableEmailAttempt(
    cacheApi: CacheApi,
    disposableApi: DisposableEmailDomain
):

  import DisposableEmailAttempt.*

  private val byIp =
    cacheApi.notLoadingSync[IpAddress, Set[Attempt]](512, "security.disposableEmailAttempt.ip"):
      _.expireAfterWrite(1.day).build()

  private val byId =
    cacheApi.notLoadingSync[UserId, Set[Attempt]](512, "security.disposableEmailAttempt.id"):
      _.expireAfterWrite(1.day).build()

  def onFail(form: Form[?], ip: IpAddress): Unit = for
    email <- form("email").value.flatMap(EmailAddress.from)
    if email.domain.exists(disposableApi.isDisposable)
    if !email.domain.exists(disposableApi.mightBeTypo)
    str <- form("username").value
    u   <- UserStr.read(str)
  yield
    val attempt = Attempt(u.id, email, ip)
    byIp.underlying.asMap.compute(ip, (_, attempts) => ~Option(attempts) + attempt)
    byId.underlying.asMap.compute(u.id, (_, attempts) => ~Option(attempts) + attempt)

  def onSuccess(user: User, email: EmailAddress, ip: IpAddress) =
    val attempts = ~byIp.getIfPresent(ip) ++ ~byId.getIfPresent(user.id)
    if attempts.sizeIs > 3 || (
        attempts.nonEmpty && email.domain.exists(d => !DisposableEmailDomain.whitelisted(d))
      )
    then
      val dispEmails = attempts.map(_.email)
      logger
        .branch("disposableEmailAttempt")
        .info(s"User ${user.username} signed up with $email after trying ${dispEmails.mkString(", ")}")

  def count(id: UserId): Int = byId.getIfPresent(id).so(_.size)

private object DisposableEmailAttempt:

  case class Attempt(id: UserId, email: EmailAddress, ip: IpAddress)
