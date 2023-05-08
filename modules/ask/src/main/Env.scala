package lila.ask

import com.softwaremill.macwire._
import com.softwaremill.tagging.@@
import lila.common.config._

@Module
final class Env(
    db: lila.db.AsyncDb @@ lila.db.YoloDb,
    timeline: lila.hub.actors.Timeline
)(using scala.concurrent.ExecutionContext, akka.actor.Scheduler):
  private lazy val askColl = db(CollName("ask"))
  lazy val api             = wire[AskApi]
