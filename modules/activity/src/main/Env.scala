package lidraughts.activity

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.hub.actorApi.round.CorresMoveEvent

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    system: akka.actor.ActorSystem,
    practiceApi: lidraughts.practice.PracticeApi,
    postApi: lidraughts.forum.PostApi,
    simulApi: lidraughts.simul.SimulApi,
    studyApi: lidraughts.study.StudyApi,
    lightUserApi: lidraughts.user.LightUserApi,
    tourLeaderApi: lidraughts.tournament.LeaderboardApi,
    getTourName: lidraughts.tournament.Tournament.ID => Option[String],
    getTeamName: lidraughts.team.Team.ID => Option[String]
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

  lazy val jsonView = new JsonView(
    lightUserApi = lightUserApi,
    getTourName = getTourName,
    getTeamName = getTeamName
  )

  system.lidraughtsBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lidraughts.game.actorApi.FinishGame(game, _, _) if !game.aborted => write game game
        case lidraughts.forum.actorApi.CreatePost(post, topic) if !topic.isStaff => write.forumPost(post, topic)
        case res: lidraughts.puzzle.Puzzle.UserResult => write puzzle res
        case prog: lidraughts.practice.PracticeProgress.OnComplete => write practice prog
        case lidraughts.simul.Simul.OnStart(simul) => write simul simul
        case CorresMoveEvent(move, Some(userId), _, _, false) => write.corresMove(move.gameId, userId)
        case lidraughts.hub.actorApi.plan.MonthInc(userId, months) => write.plan(userId, months)
        case lidraughts.hub.actorApi.relation.Follow(from, to) => write.follow(from, to)
        case lidraughts.study.actorApi.StartStudy(id) =>
          // wait some time in case the study turns private
          system.scheduler.scheduleOnce(5 minutes) { write study id }
        case lidraughts.hub.actorApi.team.CreateTeam(id, _, userId) => write.team(id, userId)
        case lidraughts.hub.actorApi.team.JoinTeam(id, userId) => write.team(id, userId)
      }
    })),
    'finishGame, 'forumPost, 'finishPuzzle, 'finishPractice, 'team,
    'startSimul, 'moveEventCorres, 'plan, 'relation, 'startStudy
  )
}

object Env {

  lazy val current: Env = "activity" boot new Env(
    db = lidraughts.db.Env.current,
    config = lidraughts.common.PlayApp loadConfig "activity",
    system = lidraughts.common.PlayApp.system,
    practiceApi = lidraughts.practice.Env.current.api,
    postApi = lidraughts.forum.Env.current.postApi,
    simulApi = lidraughts.simul.Env.current.api,
    studyApi = lidraughts.study.Env.current.api,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    tourLeaderApi = lidraughts.tournament.Env.current.leaderboardApi,
    getTourName = lidraughts.tournament.Env.current.cached.name _,
    getTeamName = lidraughts.team.Env.current.cached.name _
  )
}
