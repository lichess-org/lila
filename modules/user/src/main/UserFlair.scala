package lila.user

import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import lila.common.config

opaque type UserFlair = String
object UserFlair extends OpaqueString[UserFlair]:
  private[user] var list = List.empty[UserFlair]
  def all                = list

final private class UserFlairApi(ws: StandaloneWSClient, assetBaseUrl: config.AssetBaseUrl)(using Executor)(
    using scheduler: akka.actor.Scheduler
):

  private val listUrl = s"$assetBaseUrl/assets/lifat/flairs/list.txt"

  private def refresh: Funit =
    ws.url(listUrl)
      .get()
      .map:
        case res if res.status == 200 => UserFlair.list = UserFlair from res.body[String].linesIterator.toList
        case _                        => logger.error(s"Cannot fetch $listUrl")

  scheduler.scheduleWithFixedDelay(44 seconds, 11 minutes): () =>
    refresh
