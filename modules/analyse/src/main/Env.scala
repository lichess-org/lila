package lila.analyse

import com.softwaremill.macwire._

import lila.common.config._
import lila.hub.actors.GameSearch

final class Env(
    db: lila.db.Env,
    gameRepo: lila.game.GameRepo,
    indexer: GameSearch,
    net: NetConfig
) {

  lazy val analysisRepo = new AnalysisRepo(db(CollName("analysis2")))

  lazy val requesterApi = new RequesterApi(db(CollName("analysis_requester")))

  lazy val analyser = wire[Analyser]

  lazy val annotator = new Annotator(net.domain)
}
