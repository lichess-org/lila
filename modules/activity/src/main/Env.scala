package lila.activity

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: akka.actor.ActorSystem
) {

  private val activityColl = db(config getString "collection.activity")

  val write = new ActivityWriteApi(
    coll = activityColl
  )

  val read = new ActivityReadApi(
    coll = activityColl
  )

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => write game game
        case lila.analyse.actorApi.AnalysisReady(_, analysis) => write analysis analysis
        case lila.forum.actorApi.CreatePost(post, topic) => write.forumPost(post, topic)
        case res: lila.puzzle.Puzzle.UserResult => write puzzle res
        case prog: lila.practice.PracticeProgress.OnComplete => write practice prog
        case lila.simul.Simul.OnStart(simul) => write simul simul
      }
    })),
    'finishGame, 'analysisReady, 'forumPost, 'finishPuzzle, 'finishPractice, 'startSimul
  )
}

object Env {

  lazy val current: Env = "activity" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "activity",
    system = lila.common.PlayApp.system
  )
}
