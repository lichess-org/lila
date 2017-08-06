package lila.activity

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.hub.actorApi.round.CorresMoveEvent

final class Env(
    config: Config,
    db: lila.db.Env,
    system: akka.actor.ActorSystem,
    practiceApi: lila.practice.PracticeApi,
    postApi: lila.forum.PostApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi
) {

  private val activityColl = db(config getString "collection.activity")

  val write = new ActivityWriteApi(
    coll = activityColl,
    studyApi = studyApi
  )

  val read = new ActivityReadApi(
    coll = activityColl,
    practiceApi = practiceApi,
    postApi = postApi,
    simulApi = simulApi,
    studyApi = studyApi,
    tourLeaderApi = tourLeaderApi
  )

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => write game game
        case lila.forum.actorApi.CreatePost(post, topic) if !topic.isStaff => write.forumPost(post, topic)
        case res: lila.puzzle.Puzzle.UserResult => write puzzle res
        case prog: lila.practice.PracticeProgress.OnComplete => write practice prog
        case lila.simul.Simul.OnStart(simul) => write simul simul
        case CorresMoveEvent(move, Some(userId), _, _, false) => write.corresMove(move.gameId, userId)
        case lila.hub.actorApi.plan.MonthInc(userId, months) => write.plan(userId, months)
        case lila.hub.actorApi.relation.Follow(from, to) => write.follow(from, to)
        case lila.study.actorApi.StartStudy(id) =>
          // wait some time in case the study turns private
          system.scheduler.scheduleOnce(5 minutes) { write study id }
        case lila.hub.actorApi.team.CreateTeam(id, _, userId) => write.team(id, userId)
        case lila.hub.actorApi.team.JoinTeam(id, userId) => write.team(id, userId)
      }
    })),
    'finishGame, 'forumPost, 'finishPuzzle, 'finishPractice, 'team,
    'startSimul, 'moveEventCorres, 'plan, 'relation, 'startStudy
  )
}

object Env {

  lazy val current: Env = "activity" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "activity",
    system = lila.common.PlayApp.system,
    practiceApi = lila.practice.Env.current.api,
    postApi = lila.forum.Env.current.postApi,
    simulApi = lila.simul.Env.current.api,
    studyApi = lila.study.Env.current.api,
    tourLeaderApi = lila.tournament.Env.current.leaderboardApi
  )
}
