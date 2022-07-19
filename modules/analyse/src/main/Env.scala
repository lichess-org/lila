package lila.analyse

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  lazy val analysisRepo = new AnalysisRepo(db(CollName("analysis3")))

  lazy val requesterApi = new RequesterApi(db(CollName("analysis_requester")))

  lazy val analyser = wire[Analyser]

  lazy val annotator = new Annotator
}
