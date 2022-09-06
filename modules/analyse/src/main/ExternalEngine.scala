package lila.analyse

import chess.variant.Variant
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User
import lila.common.Form._
import lila.common.{ SecureRandom, ThreadLocalRandom }
import AnalyseBsonHandlers.externalEngineHandler

case class ExternalEngine(
    _id: String, // random
    engineName: String,
    maxThreads: Int,
    maxHashMib: Int,
    variants: List[String],
    officialStockfish: Boolean,   // Admissible for cloud evals
    providerSecret: String,       // Chosen at random by the provider, possibly shared between registrations
    providerData: Option[String], // Arbitrary string the provider can use to store associated data
    userId: User.ID,              // The user it has been registered for
    clientSecret: String          // Secret unique id of the registration
) {}

object ExternalEngine {

  case class FormData(
      engineName: String,
      maxThreads: Int,
      maxHashMib: Int,
      variants: Option[List[String]],
      officialStockfish: Option[Boolean],
      secret: String,
      data: Option[String]
  ) {
    def make(by: User) = ExternalEngine(
      _id = ThreadLocalRandom.nextString(12),
      engineName = engineName,
      maxThreads = maxThreads,
      maxHashMib = maxHashMib,
      variants = variants.filter(_.nonEmpty) | List(chess.variant.Standard.key),
      officialStockfish = ~officialStockfish,
      providerSecret = secret,
      providerData = data,
      userId = by.id,
      clientSecret = SecureRandom.nextString(16)
    )
  }

  val form = Form(
    mapping(
      "engineName" -> cleanNonEmptyText(3, 200),
      "maxThreads" -> number(1, 65_536),
      "maxHashMib" -> number(1, 1_048_576),
      "variants" -> optional(list {
        stringIn(chess.variant.Variant.all.filterNot(chess.variant.FromPosition ==).map(_.key).toSet)
      }),
      "officialStockfish" -> optional(boolean),
      "providerSecret"    -> nonEmptyText(8, 1024),
      "providerData"      -> optional(text(maxLength = 8192))
    )(FormData.apply)(FormData.unapply)
  )
}

final class ExternalEngineApi(coll: Coll)(implicit ec: ExecutionContext) {

  def register(by: User, data: ExternalEngine.FormData): Fu[ExternalEngine] = {
    val engine = data make by
    coll.insert.one(engine) inject engine
  }
}
