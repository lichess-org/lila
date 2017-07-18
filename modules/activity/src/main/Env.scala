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

  val api = new ActivityApi(
    coll = activityColl
  )

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => api addGame game
        case lila.analyse.actorApi.AnalysisReady(_, analysis) => api addAnalysis analysis
        case lila.forum.actorApi.CreatePost(post, topic) => api.addForumPost(post, topic)
        case res: lila.puzzle.Puzzle.UserResult => api addPuzzle res
        case prog: lila.practice.PracticeProgress.OnComplete => api addPractice prog
        case lila.simul.Simul.OnStart(simul) => api addSimul simul
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
