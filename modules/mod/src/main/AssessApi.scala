package lila.mod

import akka.actor.ActorSelection
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import lila.evaluation.{ AccountAction, Analysed, GameAssessment, PlayerAssessment, PlayerAggregateAssessment, PlayerFlags, PlayerAssessments, Assessible }
import lila.game.{ Game, Player, GameRepo }
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent._

import chess.Color

final class AssessApi(
    collAssessments: Coll,
    logApi: ModlogApi,
    modApi: ModApi,
    reporter: ActorSelection,
    analyser: ActorSelection,
    userIdsSharingIp: String => Fu[List[String]]) {

  import PlayerFlags.playerFlagsBSONHandler
  import lila.evaluation.Statistics.{ skip, moveTimeCoefVariation }

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]

  def createPlayerAssessment(assessed: PlayerAssessment) =
    collAssessments.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: String) =
    collAssessments.find(BSONDocument("_id" -> id))
      .one[PlayerAssessment]

  def getPlayerAssessmentsByUserId(userId: String, nb: Int = 100) =
    collAssessments.find(BSONDocument("userId" -> userId))
      .sort(BSONDocument("date" -> -1))
      .cursor[PlayerAssessment]
      .collect[List](nb)

  def getResultsByGameIdAndColor(gameId: String, color: Color) =
    getPlayerAssessmentById(gameId + "/" + color.name)

  def getGameResultsById(gameId: String) =
    getResultsByGameIdAndColor(gameId, Color.White) zip
      getResultsByGameIdAndColor(gameId, Color.Black) map {
        a => PlayerAssessments(a._1, a._2)
      }

  def getPlayerAggregateAssessment(userId: String, nb: Int = 100): Fu[Option[PlayerAggregateAssessment]] = {
    val relatedUsers = userIdsSharingIp(userId)
    UserRepo.byId(userId) zip
      getPlayerAssessmentsByUserId(userId, nb) zip
      relatedUsers zip
      (relatedUsers flatMap UserRepo.filterByEngine) map {
        case (((Some(user), assessedGamesHead :: assessedGamesTail), relatedUs), relatedCheaters) =>
          Some(PlayerAggregateAssessment(
            user,
            assessedGamesHead :: assessedGamesTail,
            relatedUs,
            relatedCheaters))
        case _ => none
      }
  }

  def refreshAssessByUsername(username: String): Funit = withUser(username) { user =>
    (GameRepo.gamesForAssessment(user.id, 100) flatMap { gs =>
      (gs map { g =>
        AnalysisRepo.doneById(g.id) flatMap {
          case Some(a) => onAnalysisReady(g, a, false)
          case _       => funit
        }
      }).sequenceFu.void
    }) >> assessUser(user.id)
  }

  def onAnalysisReady(game: Game, analysis: Analysis, thenAssessUser: Boolean = true): Funit = {
    val shouldAssess =
      if (game.players.exists(_.hasSuspiciousHoldAlert)) true
      else if (game.isCorrespondence) false
      else if (game.players exists consistentMoveTimes(game)) true
      else if (game.playedTurns < 40) false
      else if (game.mode.casual) false
      else true
    shouldAssess.?? {
      val assessible = Assessible(Analysed(game, analysis))
      createPlayerAssessment(assessible playerAssessment chess.White) >>
        createPlayerAssessment(assessible playerAssessment chess.Black)
    } >> ((shouldAssess && thenAssessUser) ?? {
      game.whitePlayer.userId.??(assessUser) >> game.blackPlayer.userId.??(assessUser)
    })
  }

  def assessUser(userId: String): Funit =
    getPlayerAggregateAssessment(userId) flatMap {
      case Some(playerAggregateAssessment) => playerAggregateAssessment.action match {
        case AccountAction.Engine | AccountAction.EngineAndBan =>
          modApi.autoAdjust(userId)
        case AccountAction.Report =>
          reporter ! lila.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(3))
          funit
        case AccountAction.Nothing =>
          reporter ! lila.hub.actorApi.report.Clean(userId)
          funit
      }
      case none => funit
    }

  private def moveTimes(game: Game, color: Color): List[Int] =
    skip(game.moveTimes.toList, if (color == Color.White) 0 else 1)

  private def consistentMoveTimes(game: Game)(player: Player): Boolean =
    moveTimes(game, player.color).toNel.map(moveTimeCoefVariation).fold(false)(_ < 0.5)

  def onGameReady(game: Game, white: User, black: User): Funit = {

    def manyBlurs(player: Player) =
      (player.blurs.toDouble / game.playerMoves(player.color)) >= 0.7

    def winnerGreatProgress(player: Player): Boolean = {
      game.winner ?? (player ==)
    } && game.perfType ?? { perfType =>
      player.color.fold(white, black).perfs(perfType).progress >= 100
    }

    def winnerUserOption = game.winnerColor.map(_.fold(white, black))
    def winnerNbGames = for {
      user <- winnerUserOption
      perfType <- game.perfType
    } yield user.perfs(perfType).nb

    val shouldAnalyse =
      if (!game.analysable) false
      else if (game.isCorrespondence) false
      // someone is using a bot
      else if (game.players.exists(_.hasSuspiciousHoldAlert)) true
      // stop here for casual games
      else if (!game.mode.rated) false
      // stop here for short games
      else if (game.playedTurns < 40) false
      // someone has consistent move times
      else if (game.players exists consistentMoveTimes(game)) true
      // don't analyse other bullet games
      else if (game.speed == chess.Speed.Bullet) false
      // someone blurs a lot
      else if (game.players exists manyBlurs) true
      // the winner shows a great rating progress
      else if (game.players exists winnerGreatProgress) true
      // analyse some tourney games
      else if (game.isTournament) scala.util.Random.nextInt(3) == 0
      // analyse new player games
      else if (winnerNbGames.??(10 >)) scala.util.Random.nextInt(2) == 0
      else false

    if (shouldAnalyse) analyser ! lila.hub.actorApi.ai.AutoAnalyse(game.id)

    funit
  }

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op

}
