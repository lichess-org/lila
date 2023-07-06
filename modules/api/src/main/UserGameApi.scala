package lila.api

import chess.format.Fen
import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.common.LightUser
import lila.common.paginator.Paginator
import lila.game.Game
import lila.user.User

final class UserGameApi(
    bookmarkApi: lila.bookmark.BookmarkApi,
    lightUser: lila.user.LightUserApi,
    getTournamentName: lila.tournament.GetTourName
)(using Executor):

  import lila.game.JsonView.given
  import LightUser.lightUserWrites

  def jsPaginator(pag: Paginator[Game])(using ctx: Context): Fu[JsObject] =
    for
      bookmarkedIds <- bookmarkApi.filterGameIdsBookmarkedBy(pag.currentPageResults, ctx.me)
      _             <- lightUser.preloadMany(pag.currentPageResults.flatMap(_.userIds))
      _ <- getTournamentName.preload(pag.currentPageResults.flatMap(_.tournamentId))(using ctx.lang)
    yield
      given Writes[Game] = Writes { g =>
        write(g, bookmarkedIds(g.id), ctx.me)(using ctx.lang)
      }
      Json.obj(
        "paginator" -> lila.common.paginator.PaginatorJson(pag)
      )

  private def write(g: Game, bookmarked: Boolean, as: Option[User])(using lang: Lang) =
    Json
      .obj(
        "id"        -> g.id,
        "rated"     -> g.rated,
        "variant"   -> g.variant,
        "speed"     -> g.speed.key,
        "perf"      -> g.perfKey,
        "timestamp" -> g.createdAt,
        "turns"     -> g.ply,
        "status"    -> g.status,
        "source"    -> g.source.map(_.name),
        "players" -> JsObject(g.players.mapList: p =>
          p.color.name -> Json
            .obj(
              "user"   -> p.userId.flatMap(lightUser.sync),
              "userId" -> p.userId, // for BC
              "name"   -> p.name
            )
            .add("id" -> as.exists(p.isUser).option(p.id))
            .add("aiLevel" -> p.aiLevel)
            .add("rating" -> p.rating)
            .add("ratingDiff" -> p.ratingDiff)),
        "fen"       -> Fen.writeBoard(g.board),
        "winner"    -> g.winnerColor.map(_.name),
        "bookmarks" -> g.bookmarks
      )
      .add("bookmarked" -> bookmarked)
      .add("analysed" -> g.metadata.analysed)
      .add("opening" -> g.opening)
      .add("lastMove" -> g.lastMoveKeys)
      .add("clock" -> g.clock)
      .add("correspondence" -> g.daysPerTurn.map { d =>
        Json.obj("daysPerTurn" -> d)
      })
      .add("tournament" -> g.tournamentId.map { tid =>
        Json.obj("id" -> tid, "name" -> getTournamentName.sync(tid))
      })
