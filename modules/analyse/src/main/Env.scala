package lila.analyse

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    net: NetConfig
)(implicit ec: scala.concurrent.ExecutionContext) {

  lazy val analysisRepo = new AnalysisRepo(db(CollName("analysis2")))

  lazy val requesterApi = new RequesterApi(db(CollName("analysis_requester")))

  lazy val analyser = wire[Analyser]

  lazy val annotator = new Annotator(net.domain)
}
