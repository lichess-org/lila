package lila.analyse

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config
import scala.util.{ Success, Failure }
import spray.caching.{ LruCache, Cache }

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    ai: ActorSelection,
    system: ActorSystem,
    indexer: ActorSelection,
    evaluator: ActorSelection) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val NetDomain = config getString "net.domain"
  private val CachedNbTtl = config duration "cached.nb.ttl"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private[analyse] lazy val analysisColl = db(CollectionAnalysis)

  lazy val analyser = new Analyser(ai = ai, indexer = indexer, evaluator = evaluator)

  lazy val paginator = new PaginatorBuilder(
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val annotator = new Annotator(NetDomain)

  lazy val cached = new {
    private val cache: Cache[Int] = LruCache(timeToLive = CachedNbTtl)
    def nbAnalysis: Fu[Int] = cache(true)(AnalysisRepo.count)
  }

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.ai.AutoAnalyse(gameId) =>
        analyser.getOrGenerate(gameId, "lichess", admin = true, auto = true)
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    import tube.analysisTube
    def process = {
      case "analyse" :: "typecheck" :: Nil => lila.db.Typecheck.apply[Analysis](false)
    }
  }
}

object Env {

  lazy val current = "[boot] analyse" describes new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    ai = lila.hub.Env.current.actor.ai,
    system = lila.common.PlayApp.system,
    indexer = lila.hub.Env.current.actor.gameIndexer,
    evaluator = lila.hub.Env.current.actor.evaluator)
}
