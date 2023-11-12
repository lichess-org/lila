package lila.user

import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import lila.common.config.AssetBaseUrl
import play.api.libs.json.{ JsObject, Json }

opaque type UserFlair = String

object UserFlair extends OpaqueString[UserFlair]:

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

  final class Db(val list: List[UserFlair], baseUrl: AssetBaseUrl):
    lazy val set: Set[UserFlair]                    = list.toSet
    lazy val categs: Map[CategKey, List[UserFlair]] = list.groupBy(_.takeWhile('.' != _))
    lazy val pickerJson: JsonStr = JsonStr:
      Json.stringify:
        Json.obj(
          "categories" -> categList.map: categ =>
            Json.obj(
              "id" -> categ.key,
              // "name"   -> categ.name,
              "emojis" -> categs.get(categ.key).fold(Nil)(_.map(_.value))
            ),
          "emojis" -> JsObject:
            list.map: flair =>
              flair.value -> Json.obj(
                "id"   -> flair.value,
                "name" -> flair.value,
                "skins" -> Json.arr:
                  Json.obj:
                    "src" -> s"$baseUrl/assets/lifat/flair/img/$flair.webp"
              )
        )
// {
//   "categories": [
//     {
//       "id": "people",
//       "emojis": ["grinning", "joy", "smile"]
//     }
//   ],
//   "emojis": {
//     "100": {
//       "id": "100",
//       "name": "Hundred Points",
//       "keywords": ["100", "score", "perfect", "numbers", "century", "exam", "quiz", "test", "pass"],
//       "skins": [
//         {
//           "unified": "1f4af",
//           "native": "💯",
//           "x": 28,
//           "y": 6
//         }
//       ],
//       "version": 1
//     },

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
      case res if res.status == 200 => UserFlair.updateDb(res.body[String].linesIterator, assetBaseUrl)
      case _                        => logger.error(s"Cannot fetch $listUrl")

  // scheduler.scheduleWithFixedDelay(51 seconds, 7 minutes): () =>
  scheduler.scheduleWithFixedDelay(1 seconds, 7 minutes): () =>
    refresh
