package lila.analyse

import AnalyseBsonHandlers.given
import chess.variant.Variant
import com.roundeights.hasher.Algo
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.{ Json, OWrites }
import ornicar.scalalib.{ SecureRandom, ThreadLocalRandom }

import lila.common.Form.{ *, given }
import lila.common.Json.given
import lila.db.dsl.{ list as _, *, given }
import lila.memo.CacheApi
import lila.user.User

case class ExternalEngine(
    _id: String, // random
    name: String,
    maxThreads: Int,
    maxHash: Int,
    defaultDepth: Int,
    variants: List[Variant.UciKey],
    officialStockfish: Boolean, // Admissible for cloud evals
    providerSelector: String, // Hash of random secret chosen by the provider, possibly shared between registrations
    providerData: Option[String], // Arbitrary string the provider can use to store associated data
    userId: UserId,               // The user it has been registered for
    clientSecret: String          // Secret unique id of the registration
) {}

object ExternalEngine:

  case class FormData(
      name: String,
      maxThreads: Int,
      maxHash: Int,
      defaultDepth: Int,
      variants: Option[List[Variant.UciKey]],
      officialStockfish: Option[Boolean],
      providerSecret: String,
      providerData: Option[String]
  ):
    def make(userId: UserId) = ExternalEngine(
      _id = s"eei_${ThreadLocalRandom.nextString(12)}",
      name = name,
      maxThreads = maxThreads,
      maxHash = maxHash,
      defaultDepth = defaultDepth,
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
      "defaultDepth"      -> number(0, 246),
      "variants"          -> optional(list(typeIn(Variant.list.all.map(_.uciKey).toSet))),
      "officialStockfish" -> optional(boolean),
      "providerSecret"    -> nonEmptyText(16, 1024),
      "providerData"      -> optional(text(maxLength = 8192))
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
        "defaultDepth" -> e.defaultDepth,
        "variants"     -> e.variants,
        "providerData" -> e.providerData,
        "clientSecret" -> e.clientSecret
      )
      .add("officialStockfish" -> e.officialStockfish)
  }

final class ExternalEngineApi(coll: Coll, cacheApi: CacheApi)(using Executor):

  private val userCache = cacheApi[UserId, List[ExternalEngine]](65_536, "externalEngine.user") {
    _.maximumSize(65_536).buildAsyncFuture(doFetchList)
  }
  import lila.db.dsl.list
  private def doFetchList(userId: UserId) = coll.list[ExternalEngine]($doc("userId" -> userId), 64)
  private def reloadCache(userId: UserId) = userCache.put(userId, doFetchList(userId))

  def list(by: User): Fu[List[ExternalEngine]] = userCache get by.id

  def create(by: User, data: ExternalEngine.FormData, oauthTokenId: String): Fu[ExternalEngine] =
    val engine = data make by.id
    val bson = {
      engineHandler writeOpt engine err "external engine bson"
    } ++ $doc("oauthToken" -> oauthTokenId)
    coll.insert.one(bson) andDo reloadCache(by.id) inject engine

  def find(by: User, id: String): Fu[Option[ExternalEngine]] =
    list(by).map(_.find(_._id == id))

  def update(prev: ExternalEngine, data: ExternalEngine.FormData): Fu[ExternalEngine] =
    val engine = data update prev
    coll.update.one($id(engine._id), engine) andDo reloadCache(engine.userId) inject engine

  def delete(by: User, id: String): Fu[Boolean] =
    coll.delete.one($doc("userId" -> by.id) ++ $id(id)) map { result =>
      reloadCache(by.id)
      result.n > 0
    }

  private[analyse] def onTokenRevoke(id: String) =
    coll.primitiveOne[UserId]($doc("oauthToken" -> id), "userId") flatMapz { userId =>
      coll.delete.one($doc("oauthToken" -> id)).void andDo reloadCache(userId)
    }
