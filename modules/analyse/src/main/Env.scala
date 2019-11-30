package lila.analyse

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    indexer: ActorSelection
) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val CollectionRequester = config getString "collection.requester"
  private val NetDomain = config getString "net.domain"

  lazy val analysisColl = db(CollectionAnalysis)

  lazy val requesterApi = new RequesterApi(db(CollectionRequester))

  lazy val analyser = new Analyser(
    indexer = indexer,
    requesterApi = requesterApi
  )

  lazy val annotator = new Annotator(NetDomain)
}

object Env {

  lazy val current = "analyse" boot new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    indexer = lila.hub.Env.current.gameSearch
  )
}
