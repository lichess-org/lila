package lila.puzzle

import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.User
import Puzzle.{ BSONFields => F }

final private[puzzle] class PuzzleApi(
    puzzleColl: AsyncColl,
    roundColl: AsyncColl,
    voteColl: AsyncColl,
    headColl: AsyncColl,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Puzzle.puzzleBSONHandler

  object puzzle {

    def find(id: PuzzleId): Fu[Option[Puzzle]] =
      puzzleColl(_.find($doc(F.id -> id)).one[Puzzle])

    def findMany(ids: List[PuzzleId]): Fu[List[Option[Puzzle]]] =
      puzzleColl(_.optionsByOrderedIds[Puzzle, PuzzleId](ids)(_.id))

    def latest(nb: Int): Fu[List[Puzzle]] =
      puzzleColl {
        _.find($empty)
          .sort($doc(F.date -> -1))
          .cursor[Puzzle]()
          .list(nb)
      }

    val cachedLastId = cacheApi.unit[Int] {
      _.refreshAfterWrite(1 day)
        .buildAsyncFuture { _ =>
          puzzleColl(lila.db.Util.findNextId) dmap (_ - 1)
        }
    }

    def disable(id: PuzzleId): Funit =
      puzzleColl {
        _.update
          .one(
            $id(id),
            $doc("$set" -> $doc(F.vote -> AggregateVote.disable))
          )
          .void
      }
  }

  object round {

    def add(a: Round) = roundColl(_.insert.one(a))

    def upsert(a: Round) = roundColl(_.update.one($id(a.id), a, upsert = true))

    def addDenormalizedUser(a: Round, user: User) = roundColl(_.updateField($id(a.id), "u", user.id).void)

    def reset(user: User) =
      roundColl {
        _.delete.one(
          $doc(
            Round.BSONFields.id $startsWith s"${user.id}:"
          )
        )
      }
  }

  object vote {

    def value(id: PuzzleId, user: User): Fu[Option[Boolean]] =
      voteColl {
        _.primitiveOne[Boolean]($id(Vote.makeId(id, user.id)), "v")
      }

    def find(id: PuzzleId, user: User): Fu[Option[Vote]] =
      voteColl {
        _.byId[Vote](Vote.makeId(id, user.id))
      }

    def update(id: PuzzleId, user: User, v1: Option[Vote], v: Boolean): Fu[(Puzzle, Vote)] =
      puzzle find id orFail s"Can't vote for non existing puzzle $id" flatMap { p1 =>
        val (p2, v2) = v1 match {
          case Some(from) =>
            (
              (p1 withVote (_.change(from.value, v))),
              from.copy(v = v)
            )
          case None =>
            (
              (p1 withVote (_ add v)),
              Vote(Vote.makeId(id, user.id), v)
            )
        }
        voteColl {
          _.update
            .one(
              $id(v2.id),
              $set("v" -> v),
              upsert = true
            )
            .void
            .recover(lila.db.recoverDuplicateKey { _ => () })
        } zip
          puzzleColl {
            _.update
              .one($id(p2.id), $set(F.vote -> p2.vote))
          } inject (p2 -> v2)
      }
  }

  object head {

    def find(user: User): Fu[Option[PuzzleHead]] = headColl(_.byId[PuzzleHead](user.id))

    def set(h: PuzzleHead) = headColl(_.update.one($id(h.id), h, upsert = true).void)

    def addNew(user: User, puzzleId: PuzzleId) = set(PuzzleHead(user.id, puzzleId.some, puzzleId))

    def currentPuzzleId(user: User): Fu[Option[PuzzleId]] =
      find(user) dmap2 { (h: PuzzleHead) =>
        h.current | h.last
      }

    private[puzzle] def solved(user: User, id: PuzzleId): Funit =
      head find user flatMap { headOption =>
        set {
          PuzzleHead(user.id, none, headOption.fold(id)(head => id atLeast head.last))
        }
      }
  }
}
