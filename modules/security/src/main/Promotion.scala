package lila.security

import com.github.blemale.scaffeine.Cache
import lila.user.Me
import lila.common.config.NetDomain

final class PromotionApi(domain: NetDomain):

  def test(text: String, prevText: Option[String] = None)(using me: Me): Boolean =
    me.isVerified || me.isAdmin || {
      val promotions = extract(text)
      promotions.isEmpty || {
        val prevTextPromotion = prevText so extract
        val prev              = ~cache.getIfPresent(me) -- prevTextPromotion
        val accept            = prev.sizeIs < 3 && !prev.exists(promotions.contains)
        if !accept then logger.info(s"Promotion @${me.username} ${identify(text) mkString ", "}")
        accept
      }
    }

  def save(text: String)(using me: Me): Unit =
    val promotions = extract(text)
    if promotions.nonEmpty
    then cache.put(me, ~cache.getIfPresent(me) ++ promotions)

  private type Id = String

  private val cache: Cache[UserId, Set[Id]] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(24 hours)
      .build[UserId, Set[Id]]()

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
      .flatMap: m =>
        Option(m group 1)
      .toSet

  private def identify(text: String): List[String] =
    regexes.flatMap(_ findAllMatchIn text).map(_.matched)
