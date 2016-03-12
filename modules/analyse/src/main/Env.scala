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
    system: ActorSystem,
    roundMap: ActorSelection,
    indexer: ActorSelection) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val NetDomain = config getString "net.domain"
  private val CachedNbTtl = config duration "cached.nb.ttl"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private[analyse] lazy val analysisColl = db(CollectionAnalysis)

  lazy val analyser = new Analyser(
    indexer = indexer,
    roundMap = roundMap,
    bus = system.lilaBus)

  lazy val annotator = new Annotator(NetDomain)
}

object Env {

  lazy val current = "analyse" boot new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    roundMap = lila.hub.Env.current.actor.roundMap,
    indexer = lila.hub.Env.current.actor.gameSearch)
}
