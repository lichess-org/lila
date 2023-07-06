package lila.bookmark

import lila.common.paginator.*
import lila.db.dsl.{ *, given }
import lila.game.Game
import lila.game.GameRepo
import lila.user.User

final class PaginatorBuilder(
    coll: Coll,
    gameRepo: GameRepo
)(using Executor):

  def byUser(user: User, page: Int): Fu[Paginator[Game]] =
    Paginator(
      new UserAdapter(user),
      currentPage = page,
      maxPerPage = lila.common.config.MaxPerPage(12)
    )

  final class UserAdapter(user: User) extends AdapterLike[Game]:

    def nbResults: Fu[Int] = coll countSel selector

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
                  "from"         -> gameRepo.coll.name,
                  "as"           -> "game",
                  "localField"   -> "g",
                  "foreignField" -> "_id"
                )
              )
            ),
            Unwind("game"),
            ReplaceRootField("game")
          )
        .map:
          _.flatMap(lila.game.BSONHandlers.gameBSONHandler.readOpt)

    private def selector = $doc("u" -> user.id)
