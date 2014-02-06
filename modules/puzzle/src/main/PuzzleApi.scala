package lila.puzzle

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import reactivemongo.bson.{ BSONDocument, BSONInteger }
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[puzzle] final class PuzzleApi(
    puzzleColl: Coll,
    attemptColl: Coll,
    apiToken: String) {

  import Puzzle.puzzleBSONHandler

  def find(id: PuzzleId): Fu[Option[Puzzle]] =
    puzzleColl.find(BSONDocument("_id" -> id)).one[Puzzle]

  def latest(nb: Int): Fu[List[Puzzle]] =
    puzzleColl.find(BSONDocument())
      .sort(BSONDocument("date" -> -1))
      .cursor[Puzzle]
      .collect[List](nb)

  def importBatch(json: JsValue, token: String): Funit =
    if (token != apiToken) fufail("Invalid API token")
    else {
      import Generated.generatedJSONRead
      Future(json.as[List[Generated]]) flatMap {
        _.map(_.toPuzzle).sequence.future flatMap insertPuzzles
      }
    }

  def insertPuzzles(puzzles: List[PuzzleId ⇒ Puzzle]): Funit = puzzles match {
    case Nil ⇒ funit
    case puzzle :: rest ⇒ findNextId flatMap { id ⇒
      (puzzleColl insert puzzle(id)) >> insertPuzzles(rest)
    }
  }

  private def findNextId: Fu[PuzzleId] =
    puzzleColl.find(BSONDocument(), BSONDocument("_id" -> true))
      .sort(BSONDocument("_id" -> -1))
      .one[BSONDocument] map {
        _ flatMap { doc ⇒ doc.getAs[Int]("_id") map (1+) } getOrElse 1
      }

  object attempt {

    def find(puzzleId: PuzzleId, userId: String): Fu[Option[Attempt]] =
      attemptColl.find(BSONDocument(
        Attempt.BSONFields.id -> Attempt.makeId(puzzleId, userId)
      )).one[Attempt]

    def add(a: Attempt) = attemptColl insert a void

    def times(puzzleId: PuzzleId): Fu[List[Int]] = attemptColl.find(
      BSONDocument(Attempt.BSONFields.puzzleId -> puzzleId),
      BSONDocument(Attempt.BSONFields.time -> true)
    ).cursor[BSONDocument].collect[List]() map2 {
        (obj: BSONDocument) ⇒ obj.getAs[Int](Attempt.BSONFields.time)
      } map (_.flatten)
  }

  def vote(puzzleId: PuzzleId, userId: String, v: Boolean) = attemptColl.update(
    BSONDocument(Attempt.BSONFields.id -> Attempt.makeId(puzzleId, userId)),
    BSONDocument(Attempt.BSONFields.vote -> v)
  ).void
}
