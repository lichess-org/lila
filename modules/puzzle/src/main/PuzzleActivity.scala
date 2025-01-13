package lila.puzzle

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer

import lila.common.Json.given
import lila.db.dsl.{ *, given }

final class PuzzleActivity(
    colls: PuzzleColls
)(using
    ec: Executor,
    system: akka.actor.ActorSystem
):

  import PuzzleActivity.*
  import BsonHandlers.given

  def stream(config: Config): Source[JsObject, ?] =
    val perSecond = MaxPerSecond(20)

    val baseQuery = $doc(PuzzleRound.BSONFields.user -> config.user.id)
    val timeQueries = List(
      config.before.map(before => $doc(PuzzleRound.BSONFields.date.$lt(before))),
      config.since.map(since => $doc(PuzzleRound.BSONFields.date.$gte(since)))
    ).flatten
    val finalQuery = baseQuery.++(timeQueries*)

    Source.futureSource:
      colls.round
        .map(_.find(finalQuery))
        .map(_.sort($sort.desc(PuzzleRound.BSONFields.date)))
        .map(_.batchSize(perSecond.value))
        .map(_.cursor[PuzzleRound](ReadPref.sec))
        .map(_.documentSource(config.max.fold(Int.MaxValue)(_.value)))
        .map(_.grouped(perSecond.value))
        .map(_.throttle(1, 1.second))
        .map(_.mapAsync(1)(enrich))
        .map(_.mapConcat(identity))

  private def enrich(rounds: Seq[PuzzleRound]): Fu[Seq[JsObject]] =
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
      max: Option[Max],
      before: Option[Instant],
      since: Option[Instant]
  )
