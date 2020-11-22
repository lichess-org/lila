package lila.puzzle

import cats.implicits._
import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.{ User, UserRepo }

final private[puzzle] class PuzzleApi(
    colls: PuzzleColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Puzzle.{ BSONFields => F }
  import BsonHandlers._

  object puzzle {

    def find(id: Puzzle.Id): Fu[Option[Puzzle]] =
      colls.puzzle(_.byId[Puzzle](id.value))

    def delete(id: Puzzle.Id): Funit =
      colls.puzzle(_.delete.one($id(id.value))).void
  }

  object round {

    def find(user: User, puzzle: Puzzle): Fu[Option[PuzzleRound]] =
      colls.round(_.byId[PuzzleRound](PuzzleRound.Id(user.id, puzzle.id).toString))

    def upsert(a: PuzzleRound) = colls.round(_.update.one($id(a.id), a, upsert = true))

    def addDenormalizedUser(a: PuzzleRound, user: User): Funit = colls.round(
      _.updateField($id(a.id), PuzzleRound.BSONFields.user, user.id).void
    )
  }

  object vote {

    // def value(id: Puzzle.Id, user: User): Fu[Option[Boolean]] =
    //   colls.round {
    //     _.primitiveOne[Boolean]($id(Vote.makeId(id, user.id)), "v")
    //   }

    // def find(id: PuzzleId, user: User): Fu[Option[Vote]] =
    //   voteColl {
    //     _.byId[Vote](Vote.makeId(id, user.id))
    //   }

    def update(id: Puzzle.Id, user: User, v: Boolean): Funit =
      colls.round {
        _.ext
          .findAndUpdate[PuzzleRound](
            $id(PuzzleRound.Id(user.id, id)),
            $set($doc(PuzzleRound.BSONFields.vote -> v))
          )
      } flatMap {
        case Some(prevRound) if prevRound.vote.fold(true)(v !=) =>
          val dir = if (v) 1 else -1
          val inc = dir * (if (prevRound.vote.isDefined) 2 else 1)
          colls.puzzle {
            _.incField($id(id), F.vote, inc).void
          }
        case _ => funit
      }
    // puzzle find id orFail s"Can't vote for non existing puzzle $id" flatMap { p1 =>
    //   val (p2, v2) = v1 match {
    //     case Some(from) =>
    //       (
    //         (p1 withVote (_.change(from.value, v))),
    //         from.copy(v = v)
    //       )
    //     case None =>
    //       (
    //         (p1 withVote (_ add v)),
    //         Vote(Vote.makeId(id, user.id), v)
    //       )
    //   }
    //   voteColl {
    //     _.update
    //       .one(
    //         $id(v2.id),
    //         $set("v" -> v),
    //         upsert = true
    //       )
    //       .void
    //       .recover(lila.db.recoverDuplicateKey { _ => () })
    //   } zip
    //     puzzleColl {
    //       _.update
    //         .one($id(p2.id), $set(F.vote -> p2.vote))
    //     } inject (p2 -> v2)
    // }
  }
}
