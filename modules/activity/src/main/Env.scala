package lila.activity

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*
import lila.core.round.CorresMoveEvent
import lila.common.Bus

@Module
final class Env(
    db: lila.db.AsyncDb @@ lila.db.YoloDb,
    practiceStudies: lila.core.practice.GetStudies,
    gameRepo: lila.core.game.GameRepo,
    forumPostApi: lila.core.forum.ForumPostApi,
    ublogApi: lila.core.ublog.UblogApi,
    simulApi: lila.core.simul.SimulApi,
    studyApi: lila.core.study.StudyApi,
    tourLeaderApi: lila.core.tournament.leaderboard.Api,
    getTourName: lila.core.tournament.GetTourName,
    teamApi: lila.core.team.TeamApi,
    swissApi: lila.core.swiss.SwissApi,
    getLightTeam: lila.core.team.LightTeam.GetterSync,
    lightUserApi: lila.core.user.LightUserApi,
    userApi: lila.core.user.UserApi
)(using ec: Executor, scheduler: Scheduler):

  private lazy val coll = db(CollName("activity2")).failingSilently()

  lazy val write: ActivityWriteApi = wire[ActivityWriteApi]

  lazy val read: ActivityReadApi = wire[ActivityReadApi]

  lazy val jsonView = wire[JsonView]

  Bus.subscribeFuns(
    "finishGame" -> {
      case lila.core.game.FinishGame(game, _) if !game.aborted => write.game(game)
    },
    "finishPuzzle" -> { case res: lila.puzzle.Puzzle.UserResult =>
      write.puzzle(res)
    },
    "stormRun" -> { case lila.core.misc.puzzle.StormRun(userId, score) =>
      write.storm(userId, score)
    },
    "racerRun" -> { case lila.core.misc.puzzle.RacerRun(userId, score) =>
      write.racer(userId, score)
    },
    "streakRun" -> { case lila.core.misc.puzzle.StreakRun(userId, score) =>
      write.streak(userId, score)
    }
  )

  Bus.subscribeFun(
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
    case lila.core.ublog.UblogPost.Create(post)       => write.ublogPost(post)
    case prog: lila.core.practice.OnComplete          => write.practice(prog)
    case lila.core.simul.OnStart(simul)               => write.simul(simul)
    case CorresMoveEvent(move, Some(userId), _, _, _) => write.corresMove(move.gameId, userId)
    case lila.core.misc.plan.MonthInc(userId, months) => write.plan(userId, months)
    case lila.core.relation.Follow(from, to)          => write.follow(from, to)
    case lila.core.study.StartStudy(id)               =>
      // wait some time in case the study turns private
      scheduler.scheduleOnce(5 minutes) { write.study(id) }
    case lila.core.team.TeamCreate(t)                   => write.team(t.id, t.userId)
    case lila.core.team.JoinTeam(id, userId)            => write.team(id, userId)
    case lila.core.misc.streamer.StreamStart(userId, _) => write.streamStart(userId)
    case lila.core.swiss.SwissFinish(swissId, ranking)  => write.swiss(swissId, ranking)

  Bus.chan.forumPost.subscribe:
    case lila.core.forum.CreatePost(post) => write.forumPost(post)
