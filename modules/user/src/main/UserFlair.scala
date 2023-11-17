package lila.user

import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import lila.common.config.AssetBaseUrl
import play.api.libs.json.{ JsObject, JsArray, Json }
import lila.common.AssetVersion

object UserFlairApi:

  private var db: Set[UserFlair]                            = Set.empty
  private[user] def updateDb(lines: Iterator[String]): Unit = db = UserFlair from lines.toSet

  def exists(flair: UserFlair): Boolean = db.isEmpty || db(flair)

final private class UserFlairApi(
    ws: StandaloneWSClient,
    assetBaseUrl: AssetBaseUrl
)(using Executor)(using scheduler: akka.actor.Scheduler):
  private val listUrl = s"$assetBaseUrl/assets/lifat/flair/list.txt?v=${nowSeconds}"

  private def refresh: Funit =
    ws.url(listUrl).get() map:
      case res if res.status == 200 => UserFlairApi.updateDb(res.body[String].linesIterator)
      case _                        => logger.error(s"Cannot fetch $listUrl")

  scheduler.scheduleWithFixedDelay(5 seconds, 7 minutes): () =>
    refresh
