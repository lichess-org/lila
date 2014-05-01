package lila.analyse

import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.actor.ActorSelection
import chess.format.UciDump
import chess.Replay

import lila.db.api._
import lila.game.actorApi.InsertGame
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo }
import tube.analysisTube

final class Analyser(
    ai: ActorSelection,
    indexer: ActorSelection,
    evaluator: ActorSelection) {

  def get(id: String): Fu[Option[Analysis]] = AnalysisRepo byId id flatMap evictStalled

  def getDone(id: String): Fu[Option[Analysis]] = AnalysisRepo doneById id flatMap evictStalled

  def evictStalled(oa: Option[Analysis]): Fu[Option[Analysis]] = oa ?? { a =>
    if (a.stalled) (AnalysisRepo remove a.id) inject none[Analysis] else fuccess(a.some)
  }

  def hasDone(id: String): Fu[Boolean] = getDone(id) map (_.isDefined)

  def hasMany(ids: Seq[String]): Fu[Set[String]] =
    $primitive[Analysis, String]($select byIds ids, "_id")(_.asOpt[String]) map (_.toSet)

  def getOrGenerate(id: String, userId: String, admin: Boolean, auto: Boolean = false): Fu[Analysis] = {

    def generate: Fu[Analysis] =
      admin.fold(fuccess(none), AnalysisRepo userInProgress userId) flatMap {
        _.fold(doGenerate) { progressId =>
          fufail("[analysis] %s already analyses %s, won't process %s".format(userId, progressId, id))
        }
      }

    def doGenerate: Fu[Analysis] =
      $find.byId[Game](id) map (_ filter (_.analysable)) zip
        (GameRepo initialFen id) flatMap {
          case (Some(game), initialFen) => AnalysisRepo.progress(id, userId) >> {
            Replay(game.pgnMoves mkString " ", initialFen, game.variant).fold(
              fufail(_),
              replay => {
                ai ! lila.hub.actorApi.ai.Analyse(game.id, UciDump(replay), initialFen, requestedByHuman = !auto)
                AnalysisRepo byId id flatten "Missing analysis"
              }
            )
          }
          case _ => fufail(s"[analysis] game $id is missing")
        }

    get(id) flatMap {
      _.fold(generate)(fuccess(_))
    }
  }

  def complete(id: String, data: String) =
    $find.byId[Game](id) zip get(id) zip (GameRepo initialFen id) flatMap {
      case ((Some(game), Some(a1)), initialFen) => Info decodeList data match {
        case None => fufail(s"[analysis] $data")
        case Some(infos) => Replay(game.pgnMoves mkString " ", initialFen, game.variant).fold(
          fufail(_),
          replay => UciToPgn(replay, a1 complete infos) match {
            case (analysis, errors) =>
              errors foreach { e => logwarn(s"[analysis UciToPgn] $id $e") }
              if (analysis.valid) {
                play.api.Logger("analysis").info(s"success http://lichess.org/$id")
                indexer ! InsertGame(game)
                AnalysisRepo.done(id, analysis) >>- {
                  game.userIds foreach { userId =>
                    evaluator ! lila.hub.actorApi.evaluation.Refresh(userId)
                  }
                } inject analysis
              }
              else fufail(s"[analysis] invalid $id")
          })
      }
      case _ => fufail(s"[analysis] complete non-existing $id")
    } addFailureEffect {
      _ => AnalysisRepo remove id
    }
}
