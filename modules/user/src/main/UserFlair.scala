package lila.user

import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import lila.common.config.AssetBaseUrl
import play.api.libs.json.{ JsObject, JsArray, Json }

object UserFlairApi:

  type CategKey = String
  case class FlairCateg(key: CategKey, name: String)

  val categList: List[FlairCateg] = List(
    FlairCateg("smileys", "Smileys"),
    FlairCateg("people", "People"),
    FlairCateg("nature", "Animals & Nature"),
    FlairCateg("food-drink", "Food & Drink"),
    FlairCateg("activity", "Activity"),
    FlairCateg("travel-places", "Travel & Places"),
    FlairCateg("objects", "Objects"),
    FlairCateg("symbols", "Symbols"),
    FlairCateg("flags", "Flags")
  )

  val categJsString = Json.stringify:
    JsArray:
      categList.map: c =>
        Json.obj("id" -> c.key, "name" -> c.name)

  final class Db(val list: List[UserFlair], baseUrl: AssetBaseUrl):
    lazy val set: Set[UserFlair]                    = list.toSet
    lazy val categs: Map[CategKey, List[UserFlair]] = list.groupBy(_.value.takeWhile('.' != _))

  private var _db: Db = Db(Nil, AssetBaseUrl(""))
  private[user] def updateDb(lines: Iterator[String], baseUrl: AssetBaseUrl): Unit = _db =
    Db(UserFlair from lines.toList, baseUrl)

  def db                                = _db
  def exists(flair: UserFlair): Boolean = db.list.isEmpty || db.set(flair)

final private class UserFlairApi(ws: StandaloneWSClient, assetBaseUrl: AssetBaseUrl)(using Executor)(using
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode
):
  private val listUrl = s"$assetBaseUrl/assets/lifat/flair/list.txt"

  private def refresh: Funit =
    ws.url(listUrl).get() map:
      case res if res.status == 200 => UserFlairApi.updateDb(res.body[String].linesIterator, assetBaseUrl)
      case _                        => logger.error(s"Cannot fetch $listUrl")

  scheduler.scheduleWithFixedDelay(5 seconds, 7 minutes): () =>
    refresh
