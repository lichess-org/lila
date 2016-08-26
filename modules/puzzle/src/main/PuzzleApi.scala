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
    roundColl: Coll,
    learningColl: Coll,
    voteColl: Coll,
    headColl: Coll,
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

  object round {

    def add(a: Round) = roundColl insert a void
  }

  object learning {

    def find(user: User): Fu[Option[Learning]] = learningColl.byId[Learning](user.id)

    def add(l: Learning) = learningColl insert l void

    def update(user: User, puzzle: Puzzle, data: DataForm.RoundData) = 
      if (data.isWin) solved(user, puzzle.id) else failed(user, puzzle.id)

    def solved(user: User, puzzleId: PuzzleId) = learning find user flatMap {
      case None => fuccess(none)
      case Some(l) =>
        learningColl.update(
          $id(l.id),
          l solved puzzleId)
    }

    def failed(user: User, puzzleId: PuzzleId) = learning find user flatMap {
      case None => learning add Learning(user.id, List(puzzleId), List())
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

    def update(id: PuzzleId, user: User, v1: Option[Vote], v: Boolean): Fu[(Puzzle, Vote)] = puzzle find id flatMap {
      case None => fufail(s"Can't vote for non existing puzzle ${id}")
      case Some(p1) =>
        val (p2, v2) = v1 match {
          case Some(from) => (
              (p1 withVote (_.change(from.vote, v))),
              from.copy(vote = v)
            )
          case None => (
              (p1 withVote (_ add v)), 
              Vote(Vote.makeId(id, user.id), v)
            )
        }
        voteColl.update(
          $id(v2.id),
          $doc("$set" -> $doc(Vote.BSONFields.vote -> v)),
          upsert = true) zip
          puzzleColl.update(
            $doc("_id" -> p2.id),
            $set(Puzzle.BSONFields.vote -> p2.vote)) map {
              case _ => p2 -> v2
            }
    }
  }

  object head {

    def find(user: User): Fu[Option[PuzzleHead]] = headColl.byId[PuzzleHead](user.id)

    def add(h: PuzzleHead) = headColl update(
      $id(h.id),
      h,
      upsert = true) void

    def solved(user: User, id: PuzzleId) = head find user flatMap {
      case Some(PuzzleHead(_, Some(c), _)) if c == id => headColl.update(
        $id(user.id),
        PuzzleHead(user.id, none, Some(id)))
      case _ => fuccess(none)
    }
  }
}
