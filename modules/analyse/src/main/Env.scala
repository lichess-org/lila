package lila.analyse

import lila.common.PimpedConfig._

import akka.actor.ActorRef
import com.typesafe.config.Config
import spray.caching.{ LruCache, Cache }

final class Env(
    config: Config,
    db: lila.db.Env,
    ai: ActorRef,
    nameUser: String => Fu[String]) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val NetDomain = config getString "net.domain"
  private val CachedNbTtl = config duration "cached.nb.ttl"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"

  private[analyse] lazy val analysisColl = db(CollectionAnalysis)

  lazy val analyser = new Analyser(ai = ai)

  lazy val paginator = new PaginatorBuilder(
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val annotator = new Annotator(NetDomain)

  lazy val timeChart = TimeChart(nameUser) _

  lazy val cached = new {
    private val cache: Cache[Int] = LruCache(timeToLive = CachedNbTtl)
    def nbAnalysis: Fu[Int] = cache.fromFuture(true)(AnalysisRepo.count)
  }
}

object Env {

  lazy val current = "[boot] analyse" describes new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    ai = lila.hub.Env.current.actor.ai,
    nameUser = lila.user.Env.current.usernameOrAnonymous)
}
