package lila.activity

import akka.actor._
import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.common.config._
import lila.hub.actorApi.round.CorresMoveEvent

final class Env(
    db: lila.db.Db,
    practiceApi: lila.practice.PracticeApi,
    gameRepo: lila.game.GameRepo,
    postApi: lila.forum.PostApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi,
    getTourName: lila.tournament.GetTourName,
    getTeamName: lila.team.GetTeamName
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private lazy val coll = db(CollName("activity"))

  lazy val write: ActivityWriteApi = wire[ActivityWriteApi]

  lazy val read: ActivityReadApi = wire[ActivityReadApi]

  lazy val jsonView = wire[JsonView]

  lila.common.Bus.subscribeFun(
    "finishGame",
    "forumPost",
    "finishPuzzle",
    "finishPractice",
    "team",
    "startSimul",
    "moveEventCorres",
    "plan",
    "relation",
    "startStudy",
    "streamStart"
  ) {
    case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => write game game
    case lila.forum.actorApi.CreatePost(post)                       => write.forumPost(post)
    case res: lila.puzzle.Puzzle.UserResult                         => write puzzle res
    case prog: lila.practice.PracticeProgress.OnComplete            => write practice prog
    case lila.simul.Simul.OnStart(simul)                            => write simul simul
    case CorresMoveEvent(move, Some(userId), _, _, false)           => write.corresMove(move.gameId, userId)
    case lila.hub.actorApi.plan.MonthInc(userId, months)            => write.plan(userId, months)
    case lila.hub.actorApi.relation.Follow(from, to)                => write.follow(from, to)
    case lila.study.actorApi.StartStudy(id)                         =>
      // wait some time in case the study turns private
      system.scheduler.scheduleOnce(5 minutes) { write study id }
    case lila.hub.actorApi.team.CreateTeam(id, _, userId) => write.team(id, userId)
    case lila.hub.actorApi.team.JoinTeam(id, userId)      => write.team(id, userId)
    case lila.hub.actorApi.streamer.StreamStart(userId)   => write.streamStart(userId)
    case lila.user.User.GDPRErase(user)                   => write erase user
  }
}
