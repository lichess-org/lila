package lila.activity

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.common.config.*
import lila.hub.actorApi.round.CorresMoveEvent

@Module
@annotation.nowarn("msg=unused")
final class Env(
    db: lila.db.AsyncDb @@ lila.db.YoloDb,
    practiceApi: lila.practice.PracticeApi,
    gameRepo: lila.game.GameRepo,
    forumPostApi: lila.forum.ForumPostApi,
    ublogApi: lila.ublog.UblogApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi,
    getTourName: lila.tournament.GetTourName,
    getTeamName: lila.team.GetTeamNameSync,
    teamRepo: lila.team.TeamRepo,
    swissApi: lila.swiss.SwissApi,
    lightUserApi: lila.user.LightUserApi
)(using
    ec: Executor,
    scheduler: Scheduler
):

  private lazy val coll = db(CollName("activity2")).failingSilently()

  lazy val write: ActivityWriteApi = wire[ActivityWriteApi]

  lazy val read: ActivityReadApi = wire[ActivityReadApi]

  lazy val jsonView = wire[JsonView]

  lila.common.Bus.subscribeFuns(
    "finishGame" -> {
      case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => write.game(game).unit
    },
    "finishPuzzle" -> { case res: lila.puzzle.Puzzle.UserResult =>
      write.puzzle(res).unit
    },
    "stormRun" -> { case lila.hub.actorApi.puzzle.StormRun(userId, score) =>
      write.storm(userId, score).unit
    },
    "racerRun" -> { case lila.hub.actorApi.puzzle.RacerRun(userId, score) =>
      write.racer(userId, score).unit
    },
    "streakRun" -> { case lila.hub.actorApi.puzzle.StreakRun(userId, score) =>
      write.streak(userId, score).unit
    }
  )

  lila.common.Bus.subscribeFun(
    "forumPost",
    "ublogPost",
    "finishPractice",
    "team",
    "startSimul",
    "moveEventCorres",
    "plan",
    "relation",
    "startStudy",
    "streamStart",
    "swissFinish"
  ) {
    case lila.forum.CreatePost(post)                      => write.forumPost(post).unit
    case lila.ublog.UblogPost.Create(post)                => write.ublogPost(post).unit
    case prog: lila.practice.PracticeProgress.OnComplete  => write.practice(prog).unit
    case lila.simul.Simul.OnStart(simul)                  => write.simul(simul).unit
    case CorresMoveEvent(move, Some(userId), _, _, false) => write.corresMove(move.gameId, userId).unit
    case lila.hub.actorApi.plan.MonthInc(userId, months)  => write.plan(userId, months).unit
    case lila.hub.actorApi.relation.Follow(from, to)      => write.follow(from, to).unit
    case lila.study.actorApi.StartStudy(id)               =>
      // wait some time in case the study turns private
      scheduler.scheduleOnce(5 minutes) { write.study(id).unit }.unit
    case lila.hub.actorApi.team.CreateTeam(id, _, userId)  => write.team(id, userId).unit
    case lila.hub.actorApi.team.JoinTeam(id, userId)       => write.team(id, userId).unit
    case lila.hub.actorApi.streamer.StreamStart(userId, _) => write.streamStart(userId).unit
    case lila.swiss.SwissFinish(swissId, ranking)          => write.swiss(swissId, ranking).unit
  }
