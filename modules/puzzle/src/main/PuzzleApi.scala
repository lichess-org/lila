package lidraughts.puzzle

import scala.collection.breakOut
import scala.concurrent.duration._
import play.api.libs.json.JsValue
import lidraughts.db.dsl._
import lidraughts.user.User
import draughts.variant.{ Standard, Variant }
import Puzzle.{ BSONFields => F }

private[puzzle] final class PuzzleApi(
    puzzleColl: Map[Variant, Coll],
    roundColl: Map[Variant, Coll],
    voteColl: Map[Variant, Coll],
    headColl: Map[Variant, Coll],
    puzzleIdMin: PuzzleId,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    apiToken: String
) {

  import Puzzle.puzzleBSONHandler

  object puzzle {

    def find(id: PuzzleId, variant: Variant): Fu[Option[Puzzle]] =
      puzzleColl(variant).find($doc(F.id -> id)).uno[Puzzle]

    def findMany(ids: List[PuzzleId], variant: Variant): Fu[List[Option[Puzzle]]] =
      puzzleColl(variant).optionsByOrderedIds[Puzzle, PuzzleId](ids)(_.id)

    val cachedLastId: Map[Variant, lidraughts.memo.AsyncCacheSingle[Int]] = lidraughts.pref.Pref.puzzleVariants.map(variant =>
      variant -> asyncCache.single(
        name = "puzzle.lastId" + variant.key,
        f = lidraughts.db.Util findNextId puzzleColl(variant) map (_ - 1),
        expireAfter = _.ExpireAfterWrite(1 day)
      ))(breakOut)

    def importOne(json: JsValue, variant: Variant): Fu[PuzzleId] = {
      import Generated.generatedJSONRead
      insertPuzzle(json.as[Generated], variant)
    }

    def insertPuzzle(generated: Generated, variant: Variant): Fu[PuzzleId] =
      lidraughts.db.Util findNextId puzzleColl(variant) flatMap { id =>
        val p = generated.toPuzzle(id, variant)
        val fenStart = p.fen.split(':').take(3).mkString(":")
        puzzleColl(variant).exists($doc(
          F.id -> $gte(puzzleIdMin),
          F.fen.$regex(fenStart.replace("/", "\\/"), ""),
          F.history.$regex(p.history.head, "")
        )) flatMap {
          case false => puzzleColl(variant) insert p inject id
          case _ => fufail(s"Duplicate puzzle $fenStart")
        }
      }

    def export(nb: Int, variant: Variant): Fu[List[Puzzle]] = List(true, false).map { mate =>
      puzzleColl(variant).find($doc(F.mate -> mate))
        .sort($doc(F.voteRatio -> -1))
        .cursor[Puzzle]().gather[List](nb / 2)
    }.sequenceFu.map(_.flatten)

    def disable(variant: Variant, id: PuzzleId): Funit =
      puzzleColl(variant).update(
        $id(id),
        $doc("$set" -> $doc(F.vote -> AggregateVote.disable))
      ).void
  }

  object round {

    def add(a: Round, variant: Variant) = roundColl(variant) insert a
  }

  object vote {

    def value(id: PuzzleId, variant: Variant, user: User): Fu[Option[Boolean]] =
      voteColl(variant).primitiveOne[Boolean]($id(Vote.makeId(id, user.id)), "v")

    def find(id: PuzzleId, variant: Variant, user: User): Fu[Option[Vote]] = voteColl(variant).byId[Vote](Vote.makeId(id, user.id))

    def update(id: PuzzleId, variant: Variant, user: User, v1: Option[Vote], v: Boolean): Fu[(Puzzle, Vote)] = puzzle.find(id, variant) flatMap {
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
        voteColl(variant).update(
          $id(v2.id),
          $set("v" -> v),
          upsert = true
        ) zip
          puzzleColl(variant).update(
            $id(p2.id),
            $set(F.vote -> p2.vote)
          ) map {
              case _ => p2 -> v2
            }
    }
  }

  object head {

    def find(user: User, variant: Variant): Fu[Option[PuzzleHead]] = headColl(variant).byId[PuzzleHead](user.id)

    def set(h: PuzzleHead, variant: Variant) = headColl(variant).update($id(h.id), h, upsert = true) void

    def addNew(user: User, puzzleId: PuzzleId, variant: Variant) = set(PuzzleHead(user.id, puzzleId.some, puzzleId), variant)

    def solved(user: User, id: PuzzleId, variant: Variant) = head.find(user, variant) flatMap {
      case Some(PuzzleHead(_, Some(c), n)) if c == id && c > n => set(
        PuzzleHead(user.id, none, id),
        variant
      )
      case Some(PuzzleHead(_, Some(c), n)) if c == id => headColl(variant) update (
        $id(user.id),
        $unset(PuzzleHead.BSONFields.current)
      )
      case _ => fuccess(none)
    }
  }
}
