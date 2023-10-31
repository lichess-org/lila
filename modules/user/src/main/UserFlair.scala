package lila.user

import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import lila.common.config

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

  final class Db(val list: List[UserFlair]):
    lazy val set: Set[UserFlair]                    = list.toSet
    lazy val categs: Map[CategKey, List[UserFlair]] = list.groupBy(_.takeWhile('.' != _))

  private var _db: Db                                       = Db(Nil)
  private[user] def updateDb(lines: Iterator[String]): Unit = _db = Db(UserFlair from lines.toList)

  def db = _db

final private class UserFlairApi(ws: StandaloneWSClient, assetBaseUrl: config.AssetBaseUrl)(using Executor)(
    using scheduler: akka.actor.Scheduler
):
  private val listUrl = s"http://l1.org/assets/lifat/flairs/list.txt"

  private def refresh: Funit =
    ws.url(s"$assetBaseUrl/assets/lifat/flairs/list.txt")
      .get()
      .map:
        case res if res.status == 200 => UserFlair.updateDb(res.body[String].linesIterator)
        case _                        => logger.error(s"Cannot fetch $listUrl")

  scheduler.scheduleWithFixedDelay(4 seconds, 11 minutes): () =>
    refresh
