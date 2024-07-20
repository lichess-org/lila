package lila.game

import play.api.i18n.Lang
import play.api.libs.json.*
import scalalib.Json.given
import scalalib.paginator.Paginator

import lila.common.Json.given
import lila.core.game.Game
import lila.game.JsonView.given
import lila.ui.Context

final class UserGameApi(
    lightUser: lila.core.user.LightUserApi,
    getTourName: => lila.core.tournament.GetTourName
)(using Executor):

  def jsPaginator(pag: Paginator[Game])(using ctx: Context): Fu[JsObject] =
    for
      _ <- lightUser.preloadMany(pag.currentPageResults.flatMap(_.userIds))
      _ <- getTourName.preload(pag.currentPageResults.flatMap(_.tournamentId))(using ctx.lang)
    yield
      given Writes[Game] = Writes: g =>
        write(g, ctx.me)(using ctx.lang)
      Json.obj("paginator" -> pag)

  private def write(g: Game, as: Option[User])(using Lang) =
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
        "fen"       -> chess.format.Fen.writeBoard(g.board),
        "winner"    -> g.winnerColor.map(_.name),
        "bookmarks" -> g.bookmarks
      )
      .add("analysed" -> g.metadata.analysed)
      .add("opening" -> g.opening)
      .add("lastMove" -> g.lastMoveKeys)
      .add("clock" -> g.clock)
      .add("correspondence" -> g.daysPerTurn.map { d =>
        Json.obj("daysPerTurn" -> d)
      })
      .add("tournament" -> g.tournamentId.map { tid =>
        Json.obj("id" -> tid, "name" -> getTourName.sync(tid))
      })
