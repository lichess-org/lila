package lila.puzzle

import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.json.JsValue
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson.BSONArray

import lila.db.dsl._
import lila.user.{ User, UserRepo }

private[puzzle] final class PuzzleApi(
    puzzleColl: Coll,
    attemptColl: Coll,
    learningColl: Coll,
    voteColl: Coll,
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
          "fen".$regex(fenStart.replace("/", "\\/"), "")
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
        $doc("$set" -> $doc(Puzzle.BSONFields.vote -> AggregateVote.disable))
      ).void
  }

  object attempt {

    def find(puzzleId: PuzzleId, userId: String): Fu[Option[Attempt]] =
      attemptColl.find($doc(
        Attempt.BSONFields.id -> Attempt.makeId(puzzleId, userId)
      )).uno[Attempt]

    def vote(a1: Attempt, v1: Option[Vote], v: Boolean): Fu[(Puzzle, Vote)] = puzzle find a1.puzzleId flatMap {
      case None => fufail(s"Can't vote for non existing puzzle ${a1.puzzleId}")
      case Some(p1) =>
        val (p2, v2) = v1 match {
          case Some(from) => (
              (p1 withVote (_.change(from.vote, v))),
              from.copy(vote = v)
            )
          case None => (
              (p1 withVote (_ add v)), 
              Vote(Vote.makeId(a1.puzzleId, a1.userId), v)
            )
        }
        voteColl.update(
          $id(v2.id),
          $doc("$set" -> $doc(Vote.BSONFields.vote -> v)),
          upsert = true) zip
          puzzleColl.update(
            $doc("_id" -> p2.id),
            $doc("$set" -> $doc(Puzzle.BSONFields.vote -> p2.vote))) map {
              case _ => p2 -> v2
            }
    }

    def add(a: Attempt) = attemptColl.insert(a) recoverWith lila.db.recoverDuplicateKey { _ =>
      attemptColl.update($id(a.id),
        $doc("$set" -> $doc(
          Attempt.BSONFields.win -> a.win,
          Attempt.BSONFields.date -> a.date,
          Attempt.BSONFields.time -> a.time,
          Attempt.BSONFields.puzzleRating -> a.puzzleRating,
          Attempt.BSONFields.puzzleRatingDiff -> a.puzzleRatingDiff,
          Attempt.BSONFields.userRating -> a.userRating,
          Attempt.BSONFields.userRatingDiff -> a.userRatingDiff
          )))
    } void

    def hasPlayed(user: User, puzzle: Puzzle): Fu[Boolean] =
      attemptColl.exists($doc(
        Attempt.BSONFields.id -> Attempt.makeId(puzzle.id, user.id)
      ))

    def playedIds(user: User): Fu[BSONArray] =
      attemptColl.distinct(Attempt.BSONFields.puzzleId,
        $doc(Attempt.BSONFields.userId -> user.id).some
      ) map BSONArray.apply
  }

  object learning {

    def find(user: User): Fu[Option[Learning]] = learningColl.byId[Learning](user.id)

    def add(l: Learning) = learningColl insert l void

    def update(user: User, puzzle: Puzzle, data: DataForm.AttemptData) = 
      if (data.isWin) solved(user, puzzle.id) else failed(user, puzzle.id)

    def solved(user: User, puzzleId: PuzzleId) = learning find user flatMap {
      case None => fuccess(none)
      case Some(l) =>
        learningColl.update(
          $id(l.id),
          $doc("$pull" -> $doc("stack" -> puzzleId)))
    }

    def failed(user: User, puzzleId: PuzzleId) = learning find user flatMap {
      case None => learning add Learning(user.id, List(puzzleId))
      case Some(l) =>
        learningColl.update(
          $id(l.id),
          l failed puzzleId) 
    }

    def nextPuzzle(user: User): Fu[Option[Puzzle]] = learning find user flatMap {
      case None => fuccess(none)
      case Some(l) => l.nextPuzzleId ?? puzzle.find
    } 
  }

  object vote {

    def find(id: PuzzleId, user: User): Fu[Option[Vote]] = voteColl.byId[Vote](Vote.makeId(id, user.id))
  }
}
