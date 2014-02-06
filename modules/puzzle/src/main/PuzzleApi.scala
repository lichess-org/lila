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
import lila.rating.Glicko
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

    def add(puzzle: Puzzle, user: User, data: DataForm.AttemptData): Fu[Attempt] = find(puzzle.id, user.id) flatMap {
      case Some(a) ⇒ fuccess(a)
      case None ⇒
        val a = new Attempt(
          id = Attempt.makeId(puzzle.id, user.id),
          puzzleId = puzzle.id,
          userId = user.id,
          date = DateTime.now,
          win = data.isWin,
          hints = data.hints,
          retries = data.retries,
          time = data.time,
          vote = none,
          puzzleRating = puzzle.rating.intRating,
          userRating = user.perfs.puzzle.intRating)
        attemptColl.insert(a) inject a zip puzzleColl.update(
          BSONDocument("_id" -> puzzle.id),
          BSONDocument("$inc" -> BSONDocument(
            Puzzle.BSONFields.attempts -> BSONInteger(1),
            Puzzle.BSONFields.wins -> BSONInteger(data.isWin ? 1 | 0)
          ))) map { _ ⇒ a }
    }
  }

  def vote(puzzleId: PuzzleId, userId: String, v: Boolean) = attemptColl.update(
    BSONDocument(Attempt.BSONFields.id -> Attempt.makeId(puzzleId, userId)),
    BSONDocument(Attempt.BSONFields.vote -> v)
  ).void

  def fixAll = puzzleColl.find(
    BSONDocument(),
    BSONDocument("history" -> true)
  ).cursor[BSONDocument].enumerate() |>>>
    (Iteratee.foldM[BSONDocument, Unit](()) {
      case (_, doc) ⇒
        val reader = new lila.db.BSON.Reader(doc)
        val (id, moves) = (reader str "_id", reader str "history" split ' ')
        Generated fenOf moves match {
          case Success(fen) ⇒ puzzleColl.update(
            BSONDocument("_id" -> id),
            BSONDocument("$set" -> BSONDocument(
              Puzzle.BSONFields.fen -> fen,
              Puzzle.BSONFields.rating -> Glicko.default,
              Puzzle.BSONFields.vote -> BSONInteger(0),
              Puzzle.BSONFields.attempts -> BSONInteger(0)
            ))
          ).void
          case Failure(err) ⇒
            println(err)
            fufail(err.getMessage)
        }
    })
}
