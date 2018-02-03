package lila.api

import play.api.libs.json._

import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.game.GameRepo
import lila.user.{ UserRepo, User }

private[api] final class UserApi(
    jsonView: lila.user.JsonView,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    crosstableApi: lila.game.CrosstableApi,
    gameCache: lila.game.Cached,
    prefApi: lila.pref.PrefApi,
    makeUrl: String => String
) {

  def pager(pag: Paginator[User]): JsObject =
    Json.obj("paginator" -> PaginatorJson(pag.mapResults { u =>
      jsonView(u) ++ Json.obj("url" -> makeUrl(s"@/${u.username}"))
    }))

  def one(username: String, isAuth: Boolean)(implicit ctx: Context): Fu[Option[JsObject]] = UserRepo named username flatMap {
    case None => fuccess(none)
    case Some(u) if u.disabled => fuccess {
      Json.obj(
        "id" -> u.id,
        "username" -> u.username,
        "closed" -> true
      ).some
    }
    case Some(u) => GameRepo mostUrgentGame u zip
      (ctx.me.filter(u!=) ?? { me => crosstableApi.nbGames(me.id, u.id) }) zip
      relationApi.countFollowing(u.id) zip
      relationApi.countFollowers(u.id) zip
      ctx.isAuth.?? { prefApi followable u.id } zip
      ctx.userId.?? { relationApi.fetchRelation(_, u.id) } zip
      ctx.userId.?? { relationApi.fetchFollows(u.id, _) } zip
      bookmarkApi.countByUser(u) zip
      gameCache.nbPlaying(u.id) zip
      gameCache.nbImportedBy(u.id) map {
        case gameOption ~ nbGamesWithMe ~ following ~ followers ~ followable ~ relation ~ isFollowed ~ nbBookmarks ~ nbPlaying ~ nbImported =>
          jsonView(u) ++ {
            Json.obj(
              "url" -> makeUrl(s"@/$username"),
              "playing" -> gameOption.map(g => makeUrl(s"${g.gameId}/${g.color.name}")),
              "nbFollowing" -> following,
              "nbFollowers" -> followers,
              "count" -> Json.obj(
                "all" -> u.count.game,
                "rated" -> u.count.rated,
                "ai" -> u.count.ai,
                "draw" -> u.count.draw,
                "drawH" -> u.count.drawH,
                "loss" -> u.count.loss,
                "lossH" -> u.count.lossH,
                "win" -> u.count.win,
                "winH" -> u.count.winH,
                "bookmark" -> nbBookmarks,
                "playing" -> nbPlaying,
                "import" -> nbImported,
                "me" -> nbGamesWithMe
              )
            ) ++ ctx.isAuth.??(Json.obj(
                "followable" -> followable,
                "following" -> relation.has(true),
                "blocking" -> relation.has(false),
                "followsYou" -> isFollowed
              ))
          }.noNull
      } map (_.some)
  }
}
