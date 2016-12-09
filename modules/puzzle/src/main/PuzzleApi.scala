package lila.puzzle

import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.json.JsValue
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._

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

    private def lastId: Fu[Int] = lila.db.Util findNextId puzzleColl map (_ - 1)

    val cachedLastId = lila.memo.AsyncCache.single(
      name = "puzzle.lastId",
      lastId, timeToLive = 5 minutes)

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
        $doc("$set" -> $doc(Puzzle.BSONFields.vote -> AggregateVote.disable))
      ).void
  }

  object round {

    def add(a: Round) = roundColl insert a void
  }

  object learning {

    private implicit val learningBSONHandler = reactivemongo.bson.Macros.handler[Learning]

    def find(user: User): Fu[Option[Learning]] = learningColl.byId[Learning](user.id)

    def add(l: Learning) = learningColl insert l void

    def update(user: User, puzzle: Puzzle, result: Result): Fu[Boolean] =
      if (result.win) solved(user, puzzle.id)
      else failed(user, puzzle.id)

    def solved(user: User, puzzleId: PuzzleId): Fu[Boolean] = learning find user flatMap {
      case None => fuccess(false)
      case Some(l) =>
        learningColl.update($id(l.id), l solved puzzleId) inject l.contains(puzzleId)
    }

    def failed(user: User, puzzleId: PuzzleId): Fu[Boolean] = learning find user flatMap {
      case None => learning add Learning(user.id, List(puzzleId), List()) inject false
      case Some(l) =>
        learningColl.update($id(l.id), l failed puzzleId) inject l.contains(puzzleId)
    }

    def nextPuzzle(user: User): Fu[Option[Puzzle]] = learning find user flatMap {
      case None    => fuccess(none)
      case Some(l) => l.nextPuzzleId ?? puzzle.find
    }
  }

  object vote {

    def value(id: PuzzleId, user: User): Fu[Option[Boolean]] =
      voteColl.primitiveOne[Boolean]($id(Vote.makeId(id, user.id)), "v")

    def find(id: PuzzleId, user: User): Fu[Option[Vote]] = voteColl.byId[Vote](Vote.makeId(id, user.id))

    def update(id: PuzzleId, user: User, v1: Option[Vote], v: Boolean): Fu[(Puzzle, Vote)] = puzzle find id flatMap {
      case None => fufail(s"Can't vote for non existing puzzle ${id}")
      case Some(p1) =>
        val (p2, v2) = v1 match {
          case Some(from) => (
            (p1 withVote (_.change(from.value, v))),
            from.copy(v = v)
          )
          case None => (
            (p1 withVote (_ add v)),
            Vote(Vote.makeId(id, user.id), v)
          )
        }
        voteColl.update(
          $id(v2.id),
          $set("v" -> v),
          upsert = true) zip
          puzzleColl.update(
            $id(p2.id),
            $set(Puzzle.BSONFields.vote -> p2.vote)) map {
              case _ => p2 -> v2
            }
    }
  }

  object head {

    def find(user: User): Fu[Option[PuzzleHead]] = headColl.byId[PuzzleHead](user.id)

    def add(h: PuzzleHead) = headColl update (
      $id(h.id),
      h,
      upsert = true) void

    def addLearning(user: User, puzzleId: PuzzleId) = headColl update (
      $id(user.id),
      $set(PuzzleHead.BSONFields.current -> puzzleId.some),
      upsert = true) void

    def addNew(user: User, puzzleId: PuzzleId) = add(PuzzleHead(user.id, puzzleId.some, puzzleId))

    def solved(user: User, id: PuzzleId) = head find user flatMap {
      case Some(PuzzleHead(_, Some(c), n)) if c == id && c > n => headColl update (
        $id(user.id),
        PuzzleHead(user.id, none, id))
      case Some(PuzzleHead(_, Some(c), n)) if c == id => headColl update (
        $id(user.id),
        $unset(PuzzleHead.BSONFields.current))
      case _ => fuccess(none)
    }
  }
}
