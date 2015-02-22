package lila.mod

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.Types.Coll
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.evaluation.{ Analysed, PlayerAssessment, GameAssessments, Assessible }
import lila.game.Game
import lila.game.{ Game, GameRepo }
import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent._

import chess.Color


final class AssessApi(collAssessments: Coll, logApi: ModlogApi) {

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]

  def createPlayerAssessment(assessed: PlayerAssessment) =
    collAssessments.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: String) = 
    collAssessments.find(BSONDocument("_id" -> id))
    .one[PlayerAssessment]

  def getPlayerAssessmentsByUserId(userId: String, nb: Int = 100) = 
    collAssessments.find(BSONDocument("userId" -> userId))
    .sort(BSONDocument("assessment" -> -1))
    .cursor[PlayerAssessment]
    .collect[List](nb)

  def getResultsByGameIdAndColor(gameId: String, color: Color) =
    collAssessments.find(BSONDocument("_id" -> (gameId + "/" + color.name)))
    .one[PlayerAssessment]

  def getGameResultsById(gameId: String) =
    getResultsByGameIdAndColor(gameId, Color.White) zip
    getResultsByGameIdAndColor(gameId, Color.Black) map {
      a => GameAssessments(a._1, a._2)
    }

  def refreshAssess(gameId: String): Funit =
    GameRepo.game(gameId) zip
      AnalysisRepo.doneById(gameId) map {
        case (Some(g), Some(a)) => onAnalysisReady(g, a)
        case _ => funit
      }

  def onAnalysisReady(game: Game, analysis: Analysis): Funit = {
    if (!game.isCorrespondence && game.turns >= 40 && game.mode.rated) {
      val gameAssessments: GameAssessments = Assessible(Analysed(game, analysis)).assessments
      gameAssessments.white.fold(funit){createPlayerAssessment} >>
      gameAssessments.black.fold(funit){createPlayerAssessment}
    } else funit
  }
}
