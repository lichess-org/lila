package lila.bookmark

import scalalib.paginator.*

import lila.core.game.{ Game, GameRepo }
import lila.db.dsl.{ *, given }

final class PaginatorBuilder(
    coll: Coll,
    gameRepo: GameRepo
)(using Executor):

  def byUser(user: User, page: Int): Fu[Paginator[Game]] =
    Paginator(
      new UserAdapter(user),
      currentPage = page,
      maxPerPage = MaxPerPage(12)
    )

  final class UserAdapter(user: User) extends AdapterLike[Game]:

    def nbResults: Fu[Int] = coll.countSel(selector)

    def slice(offset: Int, length: Int): Fu[Seq[Game]] =
      coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match(selector) -> List(
            Sort(Descending("d")),
            Skip(offset),
            Limit(length),
            Project($doc("_id" -> false, "g" -> true)),
            PipelineOperator(
              $doc(
                "$lookup" -> $doc(
                  "from" -> gameRepo.coll.name,
                  "as" -> "game",
                  "localField" -> "g",
                  "foreignField" -> "_id"
                )
              )
            ),
            Unwind("game"),
            ReplaceRootField("game")
          )
        .map:
          _.flatMap(gameRepo.gameHandler.readOpt)

    private def selector = $doc("u" -> user.id)
