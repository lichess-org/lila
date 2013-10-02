package lila.analyse

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.format.UciDump
import makeTimeout.veryLarge

import lila.db.api._
import lila.game.actorApi.InsertGame
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, PgnRepo }
import tube.analysisTube

final class Analyser(
    ai: ActorSelection,
    indexer: ActorSelection) {

  def get(id: String): Fu[Option[Analysis]] = $find.byId[Analysis](id)

  def has(id: String): Fu[Boolean] = AnalysisRepo isDone id

  def hasMany(ids: Seq[String]): Fu[Set[String]] =
    $primitive[Analysis, String]($select byIds ids, "_id")(_.asOpt[String]) map (_.toSet)

  def getOrGenerate(id: String, userId: String, admin: Boolean): Fu[Analysis] = {

    def generate: Fu[Analysis] =
      admin.fold(fuccess(none), AnalysisRepo userInProgress userId) flatMap {
        _.fold(doGenerate) { progressId ⇒
          fufail("[analysis] %s already analyses %s, won't process %s".format(userId, progressId, id))
        }
      }

    def doGenerate: Fu[Analysis] =
      $find.byId[Game](id) map (_ filter (_.analyzable)) zip
        (PgnRepo getNonEmpty id) zip
        (GameRepo initialFen id) flatMap {
          case ((Some(game), Some(pgn)), initialFen) ⇒ (for {
            _ ← AnalysisRepo.progress(id, userId)
            uciMoves ← UciDump(pgn, initialFen, game.variant).future
            analysis ← {
              ai ? lila.hub.actorApi.ai.Analyse(id, uciMoves mkString " ", initialFen)
            } mapTo manifest[Analysis]
          } yield analysis) flatFold (
            e ⇒ AnalysisRepo.fail(id, e) >> fufail[Analysis](e.getMessage),
            a ⇒ AnalysisRepo.done(id, a) >>- (indexer ! InsertGame(game)) inject a
          )
          case _ ⇒ fufail[Analysis]("[analysis] %s no game or pgn found" format (id))
        }

    AnalysisRepo doneById id flatMap {
      _.fold(generate)(fuccess(_))
    }
  }
}
