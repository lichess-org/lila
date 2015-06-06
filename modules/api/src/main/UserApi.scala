package lila.api

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.GameRepo
import lila.hub.actorApi.{ router => R }
import lila.rating.Perf
import lila.user.tube.userTube
import lila.user.{ UserRepo, User, Perfs, Profile }
import makeTimeout.short

private[api] final class UserApi(
    jsonView: lila.user.JsonView,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    crosstableApi: lila.game.CrosstableApi,
    prefApi: lila.pref.PrefApi,
    makeUrl: Any => Fu[String],
    apiToken: String,
    userIdsSharingIp: String => Fu[List[String]]) {

  def list(
    team: Option[String],
    token: Option[String],
    nb: Option[Int],
    engine: Option[Boolean]): Fu[JsObject] = (team match {
    case Some(teamId) => lila.team.MemberRepo.userIdsByTeam(teamId) flatMap UserRepo.enabledByIds
    case None => $find(pimpQB($query(
      UserRepo.enabledSelect ++ (engine ?? UserRepo.engineSelect)
    )) sort UserRepo.sortPerfDesc(lila.rating.PerfType.Standard.key), makeNb(nb, token))
  }) flatMap { users =>
    users.map(u => makeUrl(R User u.username)).sequenceFu map { urls =>
      Json.obj(
        "list" -> JsArray(
          users zip urls map {
            case (u, url) => jsonView(u, extended = team.isDefined) ++ Json.obj("url" -> url).noNull
          }
        )
      )
    }
  }

  def one(username: String, token: Option[String])(implicit ctx: Context): Fu[Option[JsObject]] = UserRepo named username flatMap {
    case None => fuccess(none)
    case Some(u) => GameRepo mostUrgentGame u zip
      makeUrl(R User username) zip
      (check(token) ?? (knownEnginesSharingIp(u.id) map (_.some))) zip
      (ctx.me.filter(u!=) ?? { me => crosstableApi.nbGames(me.id, u.id) }) zip
      relationApi.nbFollowing(u.id) zip
      relationApi.nbFollowers(u.id) zip
      ctx.isAuth.?? { prefApi followable u.id } zip
      ctx.userId.?? { relationApi.relation(_, u.id) } zip
      ctx.userId.?? { relationApi.relation(u.id, _) } flatMap {
        case ((((((((gameOption, userUrl), knownEngines), nbGamesWithMe), following), followers), followable), relation), revRelation) => gameOption ?? { g =>
          makeUrl(R.Watcher(g.gameId, g.color.name)) map (_.some)
        } map { gameUrlOption =>
          jsonView(u, extended = true) ++ {
            Json.obj(
              "url" -> userUrl,
              "playing" -> gameUrlOption,
              "knownEnginesSharingIp" -> knownEngines,
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
                "bookmark" -> bookmarkApi.countByUser(u),
                "me" -> nbGamesWithMe)
            ) ++ ctx.isAuth.??(Json.obj(
                "followable" -> followable,
                "following" -> relation.exists(true ==),
                "blocking" -> relation.exists(false ==),
                "followsYou" -> revRelation.exists(true ==)
              ))
          }.noNull
        }
      } map (_.some)
  }

  def knownEnginesSharingIp(userId: String): Fu[List[String]] =
    userIdsSharingIp(userId) flatMap UserRepo.filterByEngine

  private def makeNb(nb: Option[Int], token: Option[String]) = math.min(check(token) ? 1000 | 100, nb | 10)

  private def check(token: Option[String]) = token ?? (apiToken==)
}
