package lila.api

import play.api.libs.json._

import chess.format.Forsyth
import lila.common.LightUser
import lila.common.paginator.Paginator
import lila.game.{ Game, PerfPicker }
import lila.user.User

final class UserGameApi(
    bookmarkApi: lila.bookmark.BookmarkApi,
    lightUser: LightUser.GetterSync,
    getTournamentName: String => Option[String]
) {

  import lila.game.JsonView._
  import LightUser.lightUserWrites

  def jsPaginator(pag: Paginator[Game])(implicit ctx: Context): Fu[JsObject] =
    bookmarkApi.filterGameIdsBookmarkedBy(pag.currentPageResults, ctx.me) map { bookmarkedIds =>
      implicit val gameWriter = Writes[Game] { g =>
        write(g, bookmarkedIds(g.id), ctx.me)
      }
      Json.obj(
        "paginator" -> lila.common.paginator.PaginatorJson(pag)
      )
    }

  private def write(g: Game, bookmarked: Boolean, as: Option[User]) = Json.obj(
    "id" -> g.id,
    "rated" -> g.rated,
    "variant" -> g.variant,
    "speed" -> g.speed.key,
    "perf" -> PerfPicker.key(g),
    "timestamp" -> g.createdAt,
    "turns" -> g.turns,
    "status" -> g.status,
    "source" -> g.source.map(_.name),
    "players" -> JsObject(g.players map { p =>
      p.color.name -> Json.obj(
        "user" -> p.userId.flatMap(lightUser),
        "userId" -> p.userId, // for BC
        "name" -> p.name
      )
        .add("id" -> as.exists(p.isUser).option(p.id))
        .add("aiLevel" -> p.aiLevel)
        .add("rating" -> p.rating)
        .add("ratingDiff" -> p.ratingDiff)
    }),
    "fen" -> Forsyth.exportBoard(g.board),
    "winner" -> g.winnerColor.map(_.name),
    "bookmarks" -> g.bookmarks
  ).add("bookmarked" -> bookmarked)
    .add("analysed" -> g.metadata.analysed)
    .add("opening" -> g.opening)
    .add("lastMove" -> g.lastMoveKeys)
    .add("clock" -> g.clock)
    .add("correspondence" -> g.daysPerTurn.map { d =>
      Json.obj("daysPerTurn" -> d)
    })
    .add("tournament" -> g.tournamentId.map { tid =>
      Json.obj("id" -> tid, "name" -> getTournamentName(tid))
    })
}
