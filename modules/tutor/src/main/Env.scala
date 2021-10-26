package lila.tutor

import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.db.dsl.Coll
import lila.fishnet.{ Analyser, FishnetAwaiter }

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    fishnetAnalyser: Analyser,
    fishnetAwaiter: FishnetAwaiter
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  lazy val builder = wire[TutorReportBuilder]
}
