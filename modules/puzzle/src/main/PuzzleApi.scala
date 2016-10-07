package lila.puzzle

import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.json.JsValue
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson.{ BSONArray, BSONValue }

import lila.db.dsl._
import lila.user.{ User, UserRepo }

private[puzzle] final class PuzzleApi(
    puzzleColl: Coll,
    attemptColl: Coll,
    apiToken: String) {

  import Puzzle.puzzleBSONHandler

  object puzzle {

    def find(id: PuzzleId): Fu[Option[Puzzle]] =
      puzzleColl.find($doc("_id" -> id)).uno[Puzzle]

    def latest(nb: Int): Fu[List[Puzzle]] =
      puzzleColl.find($empty)
        .sort($doc("date" -> -1))
        .cursor[Puzzle]()
        .gather[List](nb)

    def importOne(json: JsValue, token: String): Fu[PuzzleId] =
      if (token != apiToken) fufail("Invalid API token")
      else {
        import Generated.generatedJSONRead
        insertPuzzle(json.as[Generated])
      }

    def insertPuzzle(generated: Generated): Fu[PuzzleId] =
      lila.db.Util findNextId puzzleColl flatMap { id =>
        val p = generated toPuzzle id
        val fenStart = p.fen.split(' ').take(2).mkString(" ")
        puzzleColl.exists($doc(
          "fen".$regex(fenStart.replace("/", "\\/"), ""),
          "vote.sum" -> $gt(-100)
        )) flatMap {
          case false => puzzleColl insert p inject id
          case _     => fufail("Duplicate puzzle")
        }
      }

    def export(nb: Int): Fu[List[Puzzle]] = List(true, false).map { mate =>
      puzzleColl.find($doc("mate" -> mate))
        .sort($doc(Puzzle.BSONFields.voteSum -> -1))
        .cursor[Puzzle]().gather[List](nb / 2)
    }.sequenceFu.map(_.flatten)

    def disable(id: PuzzleId): Funit =
      puzzleColl.update(
        $id(id),
        $doc("$set" -> $doc(Puzzle.BSONFields.vote -> Vote.disable))
      ).void
  }

  object attempt {

    def find(puzzleId: PuzzleId, userId: String): Fu[Option[Attempt]] =
      attemptColl.find($doc(
        Attempt.BSONFields.id -> Attempt.makeId(puzzleId, userId)
      )).uno[Attempt]

    def vote(a1: Attempt, v: Boolean): Fu[(Puzzle, Attempt)] = puzzle find a1.puzzleId flatMap {
      case None => fufail(s"Can't vote for non existing puzzle ${a1.puzzleId}")
      case Some(p1) =>
        val p2 = a1.vote match {
          case Some(from) => p1 withVote (_.change(from, v))
          case None       => p1 withVote (_ add v)
        }
        val a2 = a1.copy(vote = v.some)
        attemptColl.update(
          $id(a2.id),
          $doc("$set" -> $doc(Attempt.BSONFields.vote -> v))) zip
          puzzleColl.update(
            $doc("_id" -> p2.id),
            $doc("$set" -> $doc(Puzzle.BSONFields.vote -> p2.vote))) map {
              case _ => p2 -> a2
            }
    }

    def add(a: Attempt) = attemptColl insert a void

    def hasPlayed(user: User, puzzle: Puzzle): Fu[Boolean] =
      attemptColl.exists($doc(
        Attempt.BSONFields.id -> Attempt.makeId(puzzle.id, user.id)
      ))

    def playedIds(user: User): Fu[BSONArray] =
      attemptColl.distinct[BSONValue, List](
        Attempt.BSONFields.puzzleId,
        $doc(Attempt.BSONFields.userId -> user.id).some
      ) map BSONArray.apply

    def hasVoted(user: User): Fu[Boolean] = attemptColl.find(
      $doc(Attempt.BSONFields.userId -> user.id),
      $doc(
        Attempt.BSONFields.vote -> true,
        Attempt.BSONFields.id -> false
      )).sort($doc(Attempt.BSONFields.date -> -1))
      .cursor[Bdoc]()
      .gather[List](5) map {
        case attempts if attempts.size < 5 => true
        case attempts => attempts.foldLeft(false) {
          case (true, _)    => true
          case (false, doc) => doc.getAs[Boolean](Attempt.BSONFields.vote).isDefined
        }
      }
  }
}
