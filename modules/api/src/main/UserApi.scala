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

  def one(u: User, withOnline: Boolean): JsObject =
    addStreaming(jsonView.full(u, withOnline = withOnline, withRating = true), u.id) ++
      Json.obj("url" -> makeUrl(s"@/${u.username}")) // for app BC

  def extended(username: String, as: Option[User], withFollows: Boolean): Fu[Option[JsObject]] =
    userRepo named username flatMap {
      _ ?? { extended(_, as, withFollows) dmap some }
    }

  def extended(u: User, as: Option[User], withFollows: Boolean): Fu[JsObject] =
    if (u.disabled) fuccess(jsonView disabled u)
    else
      gameProxyRepo.urgentGames(u).dmap(_.headOption) zip
        (as.filter(u !=) ?? { me =>
          crosstableApi.nbGames(me.id, u.id)
        }) zip
        withFollows.??(relationApi.countFollowing(u.id) dmap some) zip
        withFollows.??(relationApi.countFollowers(u.id) dmap some) zip
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
          jsonView.full(u, withOnline = true, withRating = true) ++ {
            Json
              .obj(
                "url"            -> makeUrl(s"@/${u.username}"), // for app BC
                "playing"        -> gameOption.map(g => makeUrl(s"${g.gameId}/${g.color.name}")),
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
              .add("streaming", liveStreamApi.isStreaming(u.id))
              .add("nbFollowing", following)
              .add("nbFollowers", followers) ++
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

  private def addStreaming(js: JsObject, id: User.ID) =
    js.add("streaming", liveStreamApi.isStreaming(id))

  private def makeUrl(path: String): String = s"${net.baseUrl}/$path"
}
