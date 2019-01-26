package lila.api

import play.api.libs.iteratee._
import play.api.libs.json._
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.common.{ MaxPerSecond, LightUser }
import lila.db.dsl._
import lila.game.GameRepo
import lila.user.{ UserRepo, User, Title }

private[api] final class UserApi(
    jsonView: lila.user.JsonView,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    crosstableApi: lila.game.CrosstableApi,
    playBanApi: lila.playban.PlaybanApi,
    gameCache: lila.game.Cached,
    prefApi: lila.pref.PrefApi,
    isStreaming: User.ID => Boolean,
    isPlaying: User.ID => Boolean,
    isOnline: User.ID => Boolean,
    recentTitledUserIds: () => Iterable[User.ID],
    makeUrl: String => String
)(implicit system: akka.actor.ActorSystem) {

  def pagerJson(pag: Paginator[User]): JsObject =
    Json.obj("paginator" -> PaginatorJson(pag mapResults one))

  def one(u: User): JsObject =
    addPlayingStreaming(jsonView(u), u.id) ++
      Json.obj("url" -> makeUrl(s"@/${u.username}")) // for app BC

  def exportTitled(config: UserApi.Titled): Enumerator[JsObject] =
    if (config.titles.isEmpty) Enumerator.empty[JsObject]
    else if (config.online) Enumerator.enumerate(recentTitledUserIds()) &>
      Enumeratee.filter(isOnline) &>
      Enumeratee.mapM(lightUserApi.async) &>
      Enumeratee.collect {
        case Some(lu) if lu.title.exists(ut => config.titles.exists(_.value == ut)) =>
          addPlayingStreaming(LightUser.lightUserWrites.writes(lu), lu.id)
      }
    else UserRepo.idCursor(
      selector = $doc("title" $in config.titles, "enabled" -> true),
      batchSize = config.perSecond.value
    ).bulkEnumerator() &>
      lila.common.Iteratee.delay(1 second) &>
      Enumeratee.mapM { docs =>
        lightUserApi.asyncMany {
          docs.flatMap { _.getAs[User.ID]("_id") } toList
        }
      } &>
      Enumeratee.mapConcat { users =>
        users.flatten.map { u =>
          addPlayingStreaming(LightUser.lightUserWrites.writes(u), u.id)
        }
      }

  def extended(username: String, as: Option[User]): Fu[Option[JsObject]] = UserRepo named username flatMap {
    _ ?? { extended(_, as) map some }
  }

  def extended(u: User, as: Option[User]): Fu[JsObject] =
    if (u.disabled) fuccess {
      Json.obj(
        "id" -> u.id,
        "username" -> u.username,
        "closed" -> true
      )
    }
    else {
      GameRepo mostUrgentGame u zip
        (as.filter(u!=) ?? { me => crosstableApi.nbGames(me.id, u.id) }) zip
        relationApi.countFollowing(u.id) zip
        relationApi.countFollowers(u.id) zip
        as.isDefined.?? { prefApi followable u.id } zip
        as.map(_.id).?? { relationApi.fetchRelation(_, u.id) } zip
        as.map(_.id).?? { relationApi.fetchFollows(u.id, _) } zip
        bookmarkApi.countByUser(u) zip
        gameCache.nbPlaying(u.id) zip
        gameCache.nbImportedBy(u.id) zip
        playBanApi.completionRate(u.id).map(_.map { cr => math.round(cr * 100) }) map {
          case gameOption ~ nbGamesWithMe ~ following ~ followers ~ followable ~ relation ~
            isFollowed ~ nbBookmarks ~ nbPlaying ~ nbImported ~ completionRate =>
            jsonView(u) ++ {
              Json.obj(
                "url" -> makeUrl(s"@/${u.username}"), // for app BC
                "playing" -> gameOption.map(g => makeUrl(s"${g.gameId}/${g.color.name}")),
                "nbFollowing" -> following,
                "nbFollowers" -> followers,
                "completionRate" -> completionRate,
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
              ).add("streaming", isStreaming(u.id)) ++
                as.isDefined.??(Json.obj(
                  "followable" -> followable,
                  "following" -> relation.has(true),
                  "blocking" -> relation.has(false),
                  "followsYou" -> isFollowed
                ))
            }.noNull
        }
    }

  private def addPlayingStreaming(js: JsObject, id: User.ID) =
    js.add("playing", isPlaying(id))
      .add("streaming", isStreaming(id))
}

object UserApi {

  case class Titled(
      titles: List[lila.user.Title],
      online: Boolean,
      perSecond: MaxPerSecond
  )
}
