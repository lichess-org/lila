package lila.security

import lila.user.UserRepo
import lila.core.security.UserTrust

private final class UserTrustApi(
    cacheApi: lila.memo.CacheApi,
    ipTrust: IpTrust,
    sessionStore: Store,
    userRepo: UserRepo
)(using Executor)
    extends lila.core.security.UserTrustApi:

  private val cache = cacheApi[UserId, Boolean](16_384, "security.userTrust"):
    _.expireAfterWrite(30.minutes).buildAsyncFuture(computeTrust)

  def get(id: UserId): Fu[UserTrust] = UserTrust.from(cache.get(id))

  private def computeTrust(id: UserId): Fu[Boolean] =
    userRepo
      .byId(id)
      .flatMapz: user =>
        if user.isVerifiedOrAdmin then fuccess(true)
        else if user.hasTitle || user.isPatron then fuccess(true)
        else if user.createdSinceDays(30) then fuccess(true)
        else if user.count.game > 20 then fuccess(true)
        else
          sessionStore
            .openSessions(id, 3)
            .flatMap: sessions =>
              if sessions.map(_.ua).exists(UserAgentParser.trust.isSuspicious)
              then fuccess(false)
              else sessions.map(_.ip).existsM(ipTrust.isSuspicious).not
      .addEffect: trust =>
        if !trust then logger.info(s"User $id is not trusted")
