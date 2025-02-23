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
        if hasHistory(user)
        then
          lila.mon.security.userTrust(true, "history")
          fuccess(true)
        else
          sessionStore
            .openSessions(id, 3)
            .flatMap: sessions =>
              sessions.map(_.ua).find(UserAgentParser.trust.isSuspicious) match
                case Some(ua) =>
                  logger.info(s"Not trusting user $id because of suspicious user agent: $ua")
                  lila.mon.security.userTrust(false, "ua")
                  fuccess(false)
                case None =>
                  sessions
                    .map(_.ip)
                    .findM(ipTrust.isSuspicious)
                    .map:
                      case Some(ip) =>
                        logger.info(s"Not trusting user $id because of suspicious IP: $ip")
                        lila.mon.security.userTrust(false, "ip")
                        false
                      case None =>
                        lila.mon.security.userTrust(true, "new")
                        true

  private def hasHistory(user: User): Boolean =
    user.count.lossH > 5 || user.createdSinceDays(15) || !user.plan.isEmpty || user.hasTitle
