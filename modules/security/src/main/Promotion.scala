package lila.security

import scala.concurrent.duration._

import com.github.blemale.scaffeine.Cache
import lila.user.User
import lila.common.config.NetDomain

final class PromotionApi(domain: NetDomain) {

  def test(user: User)(text: String): Boolean =
    user.isVerified || user.isAdmin || {
      val promotions = extract(text)
      promotions.isEmpty || {
        val prev   = ~cache.getIfPresent(user.id)
        val accept = prev.sizeIs < 3 && !prev.exists(promotions.contains)
        if (!accept) logger.info(s"Promotion @${user.username} ${identify(text) mkString ", "}")
        accept
      }
    }

  def save(user: User, text: String): Unit = {
    val promotions = extract(text)
    if (promotions.nonEmpty) cache.put(user.id, ~cache.getIfPresent(user.id) ++ promotions)
  }

  private type Id = String

  private val cache: Cache[User.ID, Set[Id]] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(24 hours)
      .build[User.ID, Set[Id]]()

  private lazy val regexes = List(
    s"$domain/team/([\\w-]+)",
    s"$domain/tournament/(\\w+)",
    s"$domain/swiss/(\\w+)",
    s"$domain/simul/(\\w+)",
    s"$domain/study/(\\w+)",
    s"$domain/class/(\\w+)",
    """(?:youtube\.com|youtu\.be)/(?:watch)?(?:\?v=)?([^"&?/ ]{11})""",
    """youtube\.com/channel/([\w-]{24})""",
    """twitch\.tv/([a-zA-Z0-9](?:\w{2,24}+))"""
  ).map(_.r.unanchored)

  private def extract(text: String): Set[Id] =
    regexes
      .flatMap(_ findAllMatchIn text)
      .view
      .flatMap { m =>
        Option(m group 1)
      }
      .toSet

  private def identify(text: String): List[String] =
    regexes.flatMap(_ findAllMatchIn text).map(_.matched)
}
