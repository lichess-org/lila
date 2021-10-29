package lila.tutor

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import scala.concurrent.duration._

import lila.common.config
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
    system: akka.actor.ActorSystem,
    mode: play.api.Mode,
    mat: akka.stream.Materializer
) {

  private val reportColl = db(config.CollName("tutor_report")).taggedWith[ReportColl]

  lazy val builder = wire[TutorReportBuilder]
}

trait ReportColl
