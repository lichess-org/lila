package lidraughts.mod

import akka.actor.ActorSelection
import lidraughts.analyse.{ Analysis, AnalysisRepo }
import lidraughts.db.BSON.BSONJodaDateTimeHandler
import lidraughts.db.dsl._
import lidraughts.evaluation.Statistics
import lidraughts.evaluation.{ AccountAction, Analysed, PlayerAssessment, PlayerAggregateAssessment, PlayerFlags, PlayerAssessments, Assessible }
import lidraughts.game.{ Game, Player, GameRepo, Source, Pov }
import lidraughts.security.UserSpy
import lidraughts.user.{ User, UserRepo }
import lidraughts.report.{ SuspectId, ModId }

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.util.Random

import draughts.Color

final class AssessApi(
    collAssessments: Coll,
    logApi: ModlogApi,
    modApi: ModApi,
    reporter: ActorSelection,
    draughtsnet: ActorSelection
) {

  private def bottomDate = DateTime.now.minusSeconds(3600 * 24 * 30 * 6) // matches a mongo expire index

  import PlayerFlags.playerFlagsBSONHandler

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]

  def createPlayerAssessment(assessed: PlayerAssessment) =
    collAssessments.update($id(assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: String) =
    collAssessments.byId[PlayerAssessment](id)

  private def getPlayerAssessmentsByUserId(userId: String, nb: Int = 100) =
    collAssessments.find($doc("userId" -> userId))
      .sort($doc("date" -> -1))
      .cursor[PlayerAssessment](ReadPreference.secondaryPreferred)
      .gather[List](nb)

  def getResultsByGameIdAndColor(gameId: String, color: Color) =
    getPlayerAssessmentById(gameId + "/" + color.name)

  def getGameResultsById(gameId: String) =
    getResultsByGameIdAndColor(gameId, Color.White) zip
      getResultsByGameIdAndColor(gameId, Color.Black) map {
        a => PlayerAssessments(a._1, a._2)
      }

  private def getPlayerAggregateAssessment(userId: String, nb: Int = 100): Fu[Option[PlayerAggregateAssessment]] =
    UserRepo byId userId flatMap {
      _.filter(_.noBot) ?? { user =>
        getPlayerAssessmentsByUserId(userId, nb) map { games =>
          games.nonEmpty option PlayerAggregateAssessment(user, games)
        }
      }
    }

  def withGames(pag: PlayerAggregateAssessment): Fu[PlayerAggregateAssessment.WithGames] =
    GameRepo gamesFromSecondary pag.playerAssessments.map(_.gameId) map {
      PlayerAggregateAssessment.WithGames(pag, _)
    }

  def getPlayerAggregateAssessmentWithGames(userId: String, nb: Int = 100): Fu[Option[PlayerAggregateAssessment.WithGames]] =
    getPlayerAggregateAssessment(userId, nb) flatMap {
      case None => fuccess(none)
      case Some(pag) => withGames(pag).map(_.some)
    }

  def refreshAssessByUsername(username: String): Funit = withUser(username) { user =>
    if (user.isBot) funit
    else (GameRepo.gamesForAssessment(user.id, 100) flatMap { gs =>
      (gs map { g =>
        AnalysisRepo.byGame(g) flatMap {
          _ ?? { onAnalysisReady(g, _, false) }
        }
      }).sequenceFu.void
    }) >> assessUser(user.id)
  }

  def onAnalysisReady(game: Game, analysis: Analysis, thenAssessUser: Boolean = true): Funit =
    GameRepo holdAlerts game flatMap { holdAlerts =>
      def consistentMoveTimes(game: Game)(player: Player) = Statistics.moderatelyConsistentMoveTimes(Pov(game, player))
      val shouldAssess =
        if (!game.source.exists(assessableSources.contains)) false
        else if (game.mode.casual) false
        else if (Player.HoldAlert suspicious holdAlerts) true
        else if (game.isCorrespondence) false
        else if (game.players exists consistentMoveTimes(game)) true
        else if (game.playedTurns < 40) false
        else if (game.createdAt isBefore bottomDate) false
        else true
      shouldAssess.?? {
        val analysed = Analysed(game, analysis, holdAlerts)
        val assessibleWhite = Assessible(analysed, draughts.White)
        val assessibleBlack = Assessible(analysed, draughts.Black)
        createPlayerAssessment(assessibleWhite playerAssessment) >>
          createPlayerAssessment(assessibleBlack playerAssessment)
      } >> ((shouldAssess && thenAssessUser) ?? {
        game.whitePlayer.userId.??(assessUser) >> game.blackPlayer.userId.??(assessUser)
      })
    }

  def assessUser(userId: String): Funit = {
    getPlayerAggregateAssessment(userId) flatMap {
      case Some(playerAggregateAssessment) => playerAggregateAssessment.action match {
        case AccountAction.Engine | AccountAction.EngineAndBan =>
          UserRepo.getTitle(userId).flatMap {
            case None => modApi.autoMark(SuspectId(userId), ModId.Lidraughts)
            case Some(title) => fuccess {
              val reason = s"Would mark as engine, but has a $title title"
              reporter ! lidraughts.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(reason, 3))
            }
          }
        case AccountAction.Report(reason) => fuccess {
          reporter ! lidraughts.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(reason, 3))
        }
        case AccountAction.Nothing =>
          // reporter ! lidraughts.hub.actorApi.report.Clean(userId)
          funit
      }
      case _ => funit
    }
  }

  private val assessableSources: Set[Source] = Set(Source.Lobby, Source.Pool, Source.Tournament)

  private def randomPercent(percent: Int): Boolean =
    Random.nextInt(100) < percent

  def onGameReady(game: Game, white: User, black: User): Funit = {

    import AutoAnalysis.Reason._

    def manyBlurs(player: Player) =
      game.playerBlurPercent(player.color) >= 70

    def winnerGreatProgress(player: Player): Boolean = {
      game.winner ?? (player ==)
    } && game.perfType ?? { perfType =>
      player.color.fold(white, black).perfs(perfType).progress >= 100
    }

    def noFastCoefVariation(player: Player): Option[Float] =
      Statistics.noFastMoves(Pov(game, player)) ?? Statistics.moveTimeCoefVariation(Pov(game, player))

    def winnerUserOption = game.winnerColor.map(_.fold(white, black))
    def winnerNbGames = for {
      user <- winnerUserOption
      perfType <- game.perfType
    } yield user.perfs(perfType).nb

    def suspCoefVariation(c: Color) = {
      val x = noFastCoefVariation(game player c)
      x.filter(_ < 0.45f) orElse x.filter(_ < 0.5f).ifTrue(Random.nextBoolean)
    }
    lazy val whiteSuspCoefVariation = suspCoefVariation(draughts.White)
    lazy val blackSuspCoefVariation = suspCoefVariation(draughts.Black)

    val shouldAnalyse: Fu[Option[AutoAnalysis.Reason]] =
      if (!game.analysable) fuccess(none)
      else if (!game.source.exists(assessableSources.contains)) fuccess(none)
      // give up on correspondence games
      else if (game.isCorrespondence) fuccess(none)
      // stop here for short games
      else if (game.playedTurns < 36) fuccess(none)
      // stop here for long games
      else if (game.playedTurns > 90) fuccess(none)
      // stop here for casual games
      else if (!game.mode.rated) fuccess(none)
      // discard old games
      else if (game.createdAt isBefore bottomDate) fuccess(none)
      // someone is using a bot
      else GameRepo holdAlerts game map { holdAlerts =>
        if (Player.HoldAlert suspicious holdAlerts) HoldAlert.some
        // white has consistent move times
        else if (whiteSuspCoefVariation.isDefined && randomPercent(70)) whiteSuspCoefVariation.map(_ => WhiteMoveTime)
        // black has consistent move times
        else if (blackSuspCoefVariation.isDefined && randomPercent(70)) blackSuspCoefVariation.map(_ => BlackMoveTime)
        // don't analyse half of other bullet games
        else if (game.speed == draughts.Speed.Bullet && randomPercent(50)) none
        // someone blurs a lot
        else if (game.players exists manyBlurs) Blurs.some
        // the winner shows a great rating progress
        else if (game.players exists winnerGreatProgress) WinnerRatingProgress.some
        // analyse some tourney games
        else if (game.isTournament && randomPercent(20)) TourneyRandom.some
        /// analyse new player games
        else if (winnerNbGames.??(30 >) && randomPercent(75)) NewPlayerWin.some
        else none
      }

    shouldAnalyse map {
      _ ?? { reason =>
        lidraughts.mon.cheat.autoAnalysis.reason(reason.toString)()
        draughtsnet ! lidraughts.hub.actorApi.draughtsnet.AutoAnalyse(game.id)
      }
    }
  }

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op

}
