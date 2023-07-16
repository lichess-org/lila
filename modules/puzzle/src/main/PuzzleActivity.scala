package lila.puzzle

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer

import lila.common.config.MaxPerSecond
import lila.common.Json.given
import lila.db.dsl.{ *, given }
import lila.user.User

final class PuzzleActivity(
    colls: PuzzleColls
)(using
    ec: Executor,
    system: akka.actor.ActorSystem
):

  import PuzzleActivity.*
  import BsonHandlers.given

  def stream(config: Config): Source[String, ?] =
    val perSecond = MaxPerSecond(20)
    Source futureSource:
      colls.round.map:
        _.find(
          $doc(PuzzleRound.BSONFields.user -> config.user.id) ++
            config.before.so { before =>
              $doc(PuzzleRound.BSONFields.date $lt before)
            }
        )
          .sort($sort desc PuzzleRound.BSONFields.date)
          .batchSize(perSecond.value)
          .cursor[PuzzleRound](ReadPref.sec)
          .documentSource(config.max | Int.MaxValue)
          .grouped(perSecond.value)
          .throttle(1, 1 second)
          .mapAsync(1)(enrich(config))
          .mapConcat(identity)
          .map: json =>
            s"${Json.stringify(json)}\n"

  private def enrich(config: Config)(rounds: Seq[PuzzleRound]): Fu[Seq[JsObject]] =
    colls.puzzle:
      _.optionsByOrderedIds[Puzzle, PuzzleId](
        rounds.map(_.id.puzzleId),
        readPref = _.sec
      )(_.id).map: puzzles =>
        rounds.zip(puzzles).collect { case (round, Some(puzzle)) =>
          Json.obj(
            "date"   -> round.date,
            "win"    -> round.win,
            "puzzle" -> JsonView.puzzleJsonStandalone(puzzle)
          )
        }

object PuzzleActivity:

  case class Config(
      user: User,
      max: Option[Int],
      before: Option[Instant]
  )
