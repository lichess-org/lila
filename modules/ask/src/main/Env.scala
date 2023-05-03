package lila.ask

import com.softwaremill.macwire._
import com.softwaremill.tagging.@@
import lila.common.config._

@Module
final class Env(
    db: lila.db.AsyncDb @@ lila.db.YoloDb,
    timeline: lila.hub.actors.Timeline
)(implicit ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler) {
  private lazy val askColl = db(CollName("ask"))
  //private lazy val pickColl = db(CollName("ask_picks"))
  //private lazy val feedbackColl = db(CollName("ask_feedback"))
  lazy val api          = wire[AskApi]
}
