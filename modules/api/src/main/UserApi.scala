package lila.api

import play.api.libs.json._

import lila.common.config._
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.user.User

final private[api] class UserApi(
    jsonView: lila.user.JsonView,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    crosstableApi: lila.game.CrosstableApi,
    playBanApi: lila.playban.PlaybanApi,
    gameCache: lila.game.Cached,
    userRepo: lila.user.UserRepo,
    prefApi: lila.pref.PrefApi,
    liveStreamApi: lila.streamer.LiveStreamApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    net: NetConfig
)(implicit ec: scala.concurrent.ExecutionContext) {

  def pagerJson(pag: Paginator[User]): JsObject =
    Json.obj("paginator" -> PaginatorJson(pag mapResults one))

  def one(u: User): JsObject =
    addPlayingStreaming(jsonView(u), u.id) ++
      Json.obj("url" -> makeUrl(s"@/${u.username}")) // for app BC

  def extended(username: String, as: Option[User]): Fu[Option[JsObject]] =
    userRepo named username flatMap {
      _ ?? { extended(_, as) dmap some }
    }

  def extended(u: User, as: Option[User]): Fu[JsObject] =
    if (u.disabled) fuccess {
      Json.obj(
        "id"       -> u.id,
        "username" -> u.username,
        "closed"   -> true
      )
    }
    else {
      gameProxyRepo.urgentGames(u).dmap(_.headOption) zip
        (as.filter(u !=) ?? { me =>
          crosstableApi.nbGames(me.id, u.id)
        }) zip
        relationApi.countFollowing(u.id) zip
        relationApi.countFollowers(u.id) zip
        as.isDefined.?? { prefApi followable u.id } zip
        as.map(_.id).?? { relationApi.fetchRelation(_, u.id) } zip
        as.map(_.id).?? { relationApi.fetchFollows(u.id, _) } zip
        bookmarkApi.countByUser(u) zip
        gameCache.nbPlaying(u.id) zip
        gameCache.nbImportedBy(u.id) zip
        playBanApi
          .completionRate(u.id)
          .map(_.map { cr =>
            math.round(cr * 100)
          }) map {
          // format: off
            case ((((((((((gameOption,nbGamesWithMe),following),followers),followable),
              relation),isFollowed),nbBookmarks),nbPlaying),nbImported),completionRate)=>
            // format: on
          jsonView(u) ++ {
            Json
              .obj(
                "url"            -> makeUrl(s"@/${u.username}"), // for app BC
                "playing"        -> gameOption.map(g => makeUrl(s"${g.gameId}/${g.color.name}")),
                "nbFollowing"    -> following,
                "nbFollowers"    -> followers,
                "completionRate" -> completionRate,
                "count" -> Json.obj(
                  "all"      -> u.count.game,
                  "rated"    -> u.count.rated,
                  "ai"       -> u.count.ai,
                  "draw"     -> u.count.draw,
                  "drawH"    -> u.count.drawH,
                  "loss"     -> u.count.loss,
                  "lossH"    -> u.count.lossH,
                  "win"      -> u.count.win,
                  "winH"     -> u.count.winH,
                  "bookmark" -> nbBookmarks,
                  "playing"  -> nbPlaying,
                  "import"   -> nbImported,
                  "me"       -> nbGamesWithMe
                )
              )
              .add("streaming", liveStreamApi.isStreaming(u.id)) ++
              as.isDefined.??(
                Json.obj(
                  "followable" -> followable,
                  "following"  -> relation.has(true),
                  "blocking"   -> relation.has(false),
                  "followsYou" -> isFollowed
                )
              )
          }.noNull
        }
    }

  private def addPlayingStreaming(js: JsObject, id: User.ID) =
    js.add("streaming", liveStreamApi.isStreaming(id))

  private def makeUrl(path: String): String = s"${net.baseUrl}/$path"
}
