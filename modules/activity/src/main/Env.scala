package lila.activity

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*
import lila.core.round.CorresMoveEvent

@Module
final class Env(
    db: lila.db.AsyncDb @@ lila.db.YoloDb,
    practiceStudies: lila.core.practice.GetStudies,
    gameRepo: lila.game.GameRepo,
    forumPostApi: lila.core.forum.ForumPostApi,
    ublogApi: lila.core.ublog.UblogApi,
    simulApi: lila.core.simul.SimulApi,
    studyApi: lila.core.study.StudyApi,
    tourLeaderApi: lila.core.tournament.leaderboard.Api,
    getTourName: lila.core.tournament.GetTourName,
    teamApi: lila.core.team.TeamApi,
    swissApi: lila.core.swiss.SwissApi,
    getLightTeam: lila.core.team.LightTeam.GetterSync,
    lightUserApi: lila.user.LightUserApi,
    userRepo: lila.user.UserRepo
)(using ec: Executor, scheduler: Scheduler):

  private lazy val coll = db(CollName("activity2")).failingSilently()

  lazy val write: ActivityWriteApi = wire[ActivityWriteApi]

  lazy val read: ActivityReadApi = wire[ActivityReadApi]

  lazy val jsonView = wire[JsonView]

  lila.common.Bus.subscribeFuns(
    "finishGame" -> {
      case lila.game.actorApi.FinishGame(game, _) if !game.aborted => write.game(game)
    },
    "finishPuzzle" -> { case res: lila.puzzle.Puzzle.UserResult =>
      write.puzzle(res)
    },
    "stormRun" -> { case lila.core.actorApi.puzzle.StormRun(userId, score) =>
      write.storm(userId, score)
    },
    "racerRun" -> { case lila.core.actorApi.puzzle.RacerRun(userId, score) =>
      write.racer(userId, score)
    },
    "streakRun" -> { case lila.core.actorApi.puzzle.StreakRun(userId, score) =>
      write.streak(userId, score)
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
  ):
    case lila.core.forum.CreatePost(post)                 => write.forumPost(post)
    case lila.core.ublog.UblogPost.Create(post)           => write.ublogPost(post)
    case prog: lila.core.practice.OnComplete              => write.practice(prog)
    case lila.core.simul.OnStart(simul)                   => write.simul(simul)
    case CorresMoveEvent(move, Some(userId), _, _, _)     => write.corresMove(move.gameId, userId)
    case lila.core.actorApi.plan.MonthInc(userId, months) => write.plan(userId, months)
    case lila.core.actorApi.relation.Follow(from, to)     => write.follow(from, to)
    case lila.core.study.StartStudy(id)                   =>
      // wait some time in case the study turns private
      scheduler.scheduleOnce(5 minutes) { write.study(id) }
    case lila.core.team.TeamCreate(t)                       => write.team(t.id, t.userId)
    case lila.core.team.JoinTeam(id, userId)                => write.team(id, userId)
    case lila.core.actorApi.streamer.StreamStart(userId, _) => write.streamStart(userId)
    case lila.core.swiss.SwissFinish(swissId, ranking)      => write.swiss(swissId, ranking)
