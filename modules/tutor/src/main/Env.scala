package lila.tutor

import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.db.dsl.Coll

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    divider: lila.game.Divider,
    analysisRepo: lila.analyse.AnalysisRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  lazy val builder = wire[TutorReportBuilder]
}
