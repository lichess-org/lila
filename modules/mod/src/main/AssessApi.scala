package lila.mod

import akka.actor.ActorSelection
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.evaluation.Statistics
import lila.evaluation.{ AccountAction, Analysed, PlayerAssessment, PlayerAggregateAssessment, PlayerFlags, PlayerAssessments, Assessible }
import lila.game.{ Game, Player, GameRepo, Source, Pov }
import lila.user.{ User, UserRepo }

import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.util.Random

import chess.Color

final class AssessApi(
    collAssessments: Coll,
    logApi: ModlogApi,
    modApi: ModApi,
    reporter: ActorSelection,
    fishnet: ActorSelection,
    userIdsSharingIp: String => Fu[List[String]]
) {

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

  private def getPlayerAggregateAssessment(userId: String, nb: Int = 100): Fu[Option[PlayerAggregateAssessment]] = {
    val relatedUsers = userIdsSharingIp(userId)
    UserRepo.byId(userId) zip
      getPlayerAssessmentsByUserId(userId, nb) zip
      relatedUsers zip
      (relatedUsers flatMap UserRepo.filterByEngine) map {
        case Some(user) ~ (assessedGamesHead :: assessedGamesTail) ~ relatedUs ~ relatedCheaters =>
          Some(PlayerAggregateAssessment(
            user,
            assessedGamesHead :: assessedGamesTail,
            relatedUs,
            relatedCheaters
          ))
        case _ => none
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
    (GameRepo.gamesForAssessment(user.id, 100) flatMap { gs =>
      (gs map { g =>
        AnalysisRepo.byId(g.id) flatMap {
          case Some(a) => onAnalysisReady(g, a, false)
          case _ => funit
        }
      }).sequenceFu.void
    }) >> assessUser(user.id)
  }

  def onAnalysisReady(game: Game, analysis: Analysis, thenAssessUser: Boolean = true): Funit = {
    def consistentMoveTimes(game: Game)(player: Player) = Statistics.consistentMoveTimes(Pov(game, player))
    val shouldAssess =
      if (!game.source.exists(assessableSources.contains)) false
      else if (game.mode.casual) false
      else if (game.players.exists(_.hasSuspiciousHoldAlert)) true
      else if (game.isCorrespondence) false
      else if (game.players exists consistentMoveTimes(game)) true
      else if (game.playedTurns < 40) false
      else true
    shouldAssess.?? {
      val assessible = Assessible(Analysed(game, analysis))
      createPlayerAssessment(assessible playerAssessment chess.White) >>
        createPlayerAssessment(assessible playerAssessment chess.Black)
    } >> ((shouldAssess && thenAssessUser) ?? {
      game.whitePlayer.userId.??(assessUser) >> game.blackPlayer.userId.??(assessUser)
    })
  }

  def assessUser(userId: String): Funit = {
    getPlayerAggregateAssessment(userId) flatMap {
      case Some(playerAggregateAssessment) => playerAggregateAssessment.action match {
        case AccountAction.Engine | AccountAction.EngineAndBan =>
          UserRepo.getTitle(userId).flatMap {
            case None => modApi.autoAdjust(userId)
            case Some(title) => fuccess {
              val reason = s"Would mark as engine, but has a $title title"
              reporter ! lila.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(reason, 3))
            }
          }
        case AccountAction.Report(reason) => fuccess {
          reporter ! lila.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(reason, 3))
        }
        case AccountAction.Nothing =>
          // reporter ! lila.hub.actorApi.report.Clean(userId)
          funit
      }
      case none => funit
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
    val whiteSuspCoefVariation = suspCoefVariation(chess.White)
    val blackSuspCoefVariation = suspCoefVariation(chess.Black)

    val shouldAnalyse: Option[AutoAnalysis.Reason] =
      if (!game.analysable) none
      else if (!game.source.exists(assessableSources.contains)) none
      // give up on correspondence games
      else if (game.isCorrespondence) none
      // stop here for short games
      else if (game.playedTurns < 36) none
      // stop here for long games
      else if (game.playedTurns > 90) none
      // stop here for casual games
      else if (!game.mode.rated) none
      // someone is using a bot
      else if (game.players.exists(_.hasSuspiciousHoldAlert)) HoldAlert.some
      // white has consistent move times
      else if (whiteSuspCoefVariation.isDefined && randomPercent(70)) whiteSuspCoefVariation.map(_ => WhiteMoveTime)
      // black has consistent move times
      else if (blackSuspCoefVariation.isDefined && randomPercent(70)) blackSuspCoefVariation.map(_ => BlackMoveTime)
      // don't analyse half of other bullet games
      else if (game.speed == chess.Speed.Bullet && randomPercent(50)) none
      // someone blurs a lot
      else if (game.players exists manyBlurs) Blurs.some
      // the winner shows a great rating progress
      else if (game.players exists winnerGreatProgress) WinnerRatingProgress.some
      // analyse some tourney games
      // else if (game.isTournament) randomPercent(20) option "Tourney random"
      /// analyse new player games
      else if (winnerNbGames.??(30 >) && randomPercent(75)) NewPlayerWin.some
      else none

    shouldAnalyse foreach { reason =>
      lila.mon.cheat.autoAnalysis.reason(reason.toString)()
      fishnet ! lila.hub.actorApi.fishnet.AutoAnalyse(game.id)
    }

    funit
  }

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op

}
