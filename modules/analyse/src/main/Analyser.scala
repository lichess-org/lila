package lila.analyse

import scala.concurrent.Future

import akka.actor.ActorSelection
import akka.pattern.ask
import org.joda.time.DateTime
import chess.format.UciDump
import chess.Replay

import lila.db.api._
import lila.game.actorApi.InsertGame
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo }
import makeTimeout.veryLarge
import tube.analysisTube

final class Analyser(ai: ActorSelection, indexer: ActorSelection) {

  def get(id: String): Fu[Option[Analysis]] = AnalysisRepo byId id flatMap evictStalled

  def getDone(id: String): Fu[Option[Analysis]] = AnalysisRepo doneById id flatMap evictStalled

  def evictStalled(oa: Option[Analysis]): Fu[Option[Analysis]] = oa ?? { a =>
    if (a.stalled) (AnalysisRepo remove a) inject none[Analysis] else fuccess(a.some)
  }

  def hasDone(id: String): Fu[Boolean] = getDone(id) map (_.isDefined)

  def hasMany(ids: Seq[String]): Fu[Set[String]] =
    $primitive[Analysis, String]($select byIds ids, "_id")(_.asOpt[String]) map (_.toSet)

  def getOrGenerate(id: String, userId: String, admin: Boolean): Fu[Analysis] = {

    def generate: Fu[Analysis] =
      admin.fold(fuccess(none), AnalysisRepo userInProgress userId) flatMap {
        _.fold(doGenerate) { progressId =>
          fufail("[analysis] %s already analyses %s, won't process %s".format(userId, progressId, id))
        }
      }

    def doGenerate: Fu[Analysis] =
      $find.byId[Game](id) map (_ filter (_.analysable)) zip
        (GameRepo initialFen id) flatMap {
          case (Some(game), initialFen) => (for {
            _ ← AnalysisRepo.progress(id, userId)
            replay ← Replay(game.pgnMoves mkString " ", initialFen, game.variant).future
            uciMoves = UciDump(replay)
            infos ← ai ? lila.hub.actorApi.ai.Analyse(uciMoves, initialFen) mapTo manifest[List[Info]]
            analysis = Analysis(id, infos, true, DateTime.now)
          } yield UciToPgn(replay, analysis)) flatFold (
            e => fufail[Analysis](e.getMessage), {
              case (a, errors) => {
                errors foreach { e => logwarn(s"[analyser UciToPgn] $id $e") }
                if (a.valid) {
                  AnalysisRepo.done(id, a)
                  indexer ! InsertGame(game)
                  fuccess(a)
                } else fufail(s"[analyse] invalid (empty infos): $id")
              }
            }
          )
          case _ => fufail[Analysis]("[analysis] %s no game or pgn found" format (id))
        }

    AnalysisRepo doneByIdNotOld id flatMap {
      _.fold(generate)(fuccess(_))
    }
  }
}
