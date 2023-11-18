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

  private type GetterType          = UserId => Fu[Option[UserFlair]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  private type GetterSyncType              = UserId => Option[UserFlair]
  opaque type GetterSync <: GetterSyncType = GetterSyncType
  object GetterSync extends TotalWrapper[GetterSync, GetterSyncType]

final class UserFlairApi(
    ws: StandaloneWSClient,
    assetBaseUrl: AssetBaseUrl,
    lightUserApi: LightUserApi
)(using Executor)(using scheduler: akka.actor.Scheduler):

  import UserFlairApi.*

  val getter = Getter: id =>
    lightUserApi.async(id).dmap(_.flatMap(_.flair))

  val getterSync = GetterSync: id =>
    lightUserApi.sync(id).flatMap(_.flair)

  def gettersFor(ids: List[UserId]): Fu[GetterSync] =
    lightUserApi.asyncMany(ids) map: users =>
      val pairs = for
        uOpt  <- users
        user  <- uOpt
        flair <- user.flair
      yield user.id -> flair
      val flairMap = pairs.toMap
      GetterSync(flairMap.get _)

  export lightUserApi.preloadMany

  private val listUrl = s"$assetBaseUrl/assets/lifat/flair/list.txt?v=${nowSeconds}"
  private def refresh: Funit =
    ws.url(listUrl).get() map:
      case res if res.status == 200 => UserFlairApi.updateDb(res.body[String].linesIterator)
      case _                        => logger.error(s"Cannot fetch $listUrl")

  scheduler.scheduleWithFixedDelay(5 seconds, 7 minutes): () =>
    refresh
