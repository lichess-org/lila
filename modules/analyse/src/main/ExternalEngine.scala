package lila.analyse

import chess.variant.Variant
import com.roundeights.hasher.Algo
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.{ JsObject, Json, OWrites }
import scalalib.{ SecureRandom, ThreadLocalRandom }

import lila.common.Form.{ *, given }
import lila.common.Json.given
import lila.core.misc.oauth.AccessTokenId
import lila.db.dsl.{ list as _, *, given }
import lila.memo.CacheApi

import AnalyseBsonHandlers.given

case class ExternalEngine(
    _id: String, // random
    name: String,
    maxThreads: Int,
    maxHash: Int,
    variants: List[Variant.UciKey],
    officialStockfish: Boolean, // Admissible for cloud evals
    providerSelector: String, // Hash of random secret chosen by the provider, possibly shared between registrations
    providerData: Option[String], // Arbitrary string the provider can use to store associated data
    userId: UserId,               // The user it has been registered for
    clientSecret: String          // Secret unique id of the registration
)

object ExternalEngine:

  case class FormData(
      name: String,
      maxThreads: Int,
      maxHash: Int,
      variants: Option[List[Variant.UciKey]],
      officialStockfish: Option[Boolean],
      providerSecret: String,
      providerData: Option[String],
      defaultDepth: Option[Int] // ignored for compatibility
  ):
    def make(userId: UserId) = ExternalEngine(
      _id = s"eei_${ThreadLocalRandom.nextString(12)}",
      name = name,
      maxThreads = maxThreads,
      maxHash = maxHash,
      variants = variants.filter(_.nonEmpty) | List(Variant.default.uciKey),
      officialStockfish = ~officialStockfish,
      providerSelector = Algo.sha256("providerSecret:" + providerSecret).hex,
      providerData = providerData,
      userId = userId,
      clientSecret = s"ees_${SecureRandom.nextString(16)}"
    )
    def update(engine: ExternalEngine) = make(engine.userId).copy(
      _id = engine._id,
      clientSecret = engine.clientSecret
    )

  val form = Form(
    mapping(
      "name"              -> cleanNonEmptyText(1, 200),
      "maxThreads"        -> number(1, 65_536),
      "maxHash"           -> number(1, 1_048_576),
      "variants"          -> optional(list(typeIn(Variant.list.all.map(_.uciKey).toSet))),
      "officialStockfish" -> optional(boolean),
      "providerSecret"    -> nonEmptyText(16, 1024),
      "providerData"      -> optional(text(maxLength = 8192)),
      "defaultDepth"      -> optional(number)
    )(FormData.apply)(lila.common.unapply)
  )

  given jsonWrites: OWrites[ExternalEngine] = OWrites { e =>
    Json
      .obj(
        "id"           -> e._id,
        "name"         -> e.name,
        "userId"       -> e.userId,
        "maxThreads"   -> e.maxThreads,
        "maxHash"      -> e.maxHash,
        "variants"     -> e.variants,
        "providerData" -> e.providerData,
        "clientSecret" -> e.clientSecret
      )
      .add("officialStockfish" -> e.officialStockfish)
  }

final class ExternalEngineApi(coll: Coll, cacheApi: CacheApi)(using Executor):

  private val userCache = cacheApi[UserId, List[ExternalEngine]](65_536, "externalEngine.user"):
    _.maximumSize(65_536).buildAsyncFuture(doFetchList)
  import lila.db.dsl.list
  private def doFetchList(userId: UserId) = coll.list[ExternalEngine]($doc("userId" -> userId), 64)
  private def reloadCache(userId: UserId) = userCache.put(userId, doFetchList(userId))

  def list(by: UserId): Fu[List[ExternalEngine]] = userCache.get(by)

  def create(by: UserId, data: ExternalEngine.FormData, oauthTokenId: AccessTokenId): Fu[ExternalEngine] =
    val engine = data.make(by)
    val bson   =
      engineHandler.writeOpt(engine).err("external engine bson") ++ $doc("oauthToken" -> oauthTokenId)
    for
      _ <- coll.insert.one(bson)
      _ = reloadCache(by.id)
    yield engine

  def find(by: UserId, id: String): Fu[Option[ExternalEngine]] =
    list(by).map(_.find(_._id == id))

  def update(prev: ExternalEngine, data: ExternalEngine.FormData): Fu[ExternalEngine] =
    val engine = data.update(prev)
    for
      _ <- coll.update.one($id(engine._id), engine)
      _ = reloadCache(engine.userId)
    yield engine

  def delete(by: UserId, id: String): Fu[Boolean] =
    coll.delete.one($doc("userId" -> by) ++ $id(id)).map { result =>
      reloadCache(by.id)
      result.n > 0
    }

  def withExternalEngines(json: JsObject)(using me: Option[Me]): Fu[JsObject] =
    myExternalEnginesAsJson(me).map(json ++ _)

  def myExternalEnginesAsJson(me: Option[Me]): Fu[JsObject] =
    me.so(u => list(u.userId))
      .map: engines =>
        engines.nonEmpty.so(Json.obj("externalEngines" -> engines))

  private[analyse] def onTokenRevoke(id: AccessTokenId) =
    coll.primitiveOne[UserId]($doc("oauthToken" -> id), "userId").flatMapz { userId =>
      for _ <- coll.delete.one($doc("oauthToken" -> id)) yield reloadCache(userId)
    }
