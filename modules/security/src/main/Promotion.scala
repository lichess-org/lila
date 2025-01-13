package lila.security

import com.github.blemale.scaffeine.Cache

import lila.core.config.NetDomain

final class PromotionApi(domain: NetDomain)(using Executor) extends lila.core.security.PromotionApi:

  def test(author: User, text: String, prevText: Option[String] = None): Boolean =
    Granter.ofUser(_.Verified)(author) || Granter.ofUser(_.Admin)(author) || {
      val promotions = extract(text)
      promotions.isEmpty || {
        val prevTextPromotion = prevText.so(extract)
        val prev              = ~cache.getIfPresent(author.id) -- prevTextPromotion
        val accept            = prev.sizeIs < 3 && !prev.exists(promotions.contains)
        if !accept then logger.info(s"Promotion @${author.username} ${identify(text).mkString(", ")}")
        accept
      }
    }

  def save(author: UserId, text: String): Unit =
    val promotions = extract(text)
    if promotions.nonEmpty
    then cache.put(author, ~cache.getIfPresent(author) ++ promotions)

  private type Id = String

  private val cache: Cache[UserId, Set[Id]] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(24.hours)
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
      .flatMap(_.findAllMatchIn(text))
      .view
      .flatMap: m =>
        Option(m.group(1))
      .toSet

  private def identify(text: String): List[String] =
    regexes.flatMap(_.findAllMatchIn(text)).map(_.matched)
