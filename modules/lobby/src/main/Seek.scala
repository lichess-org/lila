package lila.lobby

import chess.IntRating
import chess.rating.RatingProvisional
import chess.variant.Variant
import chess.{ Mode, Speed }
import play.api.libs.json.*
import scalalib.ThreadLocalRandom
import scalalib.model.Days

import lila.common.Json.given
import lila.core.perf.UserWithPerfs
import lila.core.rating.RatingRange
import lila.rating.PerfType

// correspondence chess, persistent
case class Seek(
    _id: String,
    variant: Variant.Id,
    daysPerTurn: Option[Days],
    mode: Int,
    user: LobbyUser,
    ratingRange: String,
    createdAt: Instant
):
  inline def id = _id

  val realVariant = Variant.orDefault(variant)

  val realMode = Mode.orDefault(mode)

  def compatibleWith(h: Seek) =
    user.id != h.user.id &&
      compatibilityProperties == h.compatibilityProperties &&
      ratingRangeCompatibleWith(h) && h.ratingRangeCompatibleWith(this)

  private def ratingRangeCompatibleWith(s: Seek) =
    realRatingRange.forall(_.contains(s.rating))

  private def compatibilityProperties = (variant, mode, daysPerTurn)

  lazy val realRatingRange: Option[RatingRange] = RatingRange.noneIfDefault(ratingRange)

  lazy val perfType = PerfType(realVariant, Speed.Correspondence)

  def perf   = user.perfAt(perfType)
  def rating = perf.rating

  def render: JsObject =
    Json
      .obj(
        "id"       -> _id,
        "username" -> user.username,
        "rating"   -> rating,
        "variant"  -> Json.obj("key" -> realVariant.key),
        "perf"     -> Json.obj("key" -> perfType.key),
        "mode"     -> realMode.id
      )
      .add("days" -> daysPerTurn)
      .add("provisional" -> perf.provisional.yes)

object Seek:

  given UserIdOf[Seek] = _.user.id

  val idSize = 8
  def makeId = ThreadLocalRandom.nextString(idSize)

  def make(
      variant: chess.variant.Variant,
      daysPerTurn: Option[Days],
      mode: Mode,
      user: UserWithPerfs,
      ratingRange: RatingRange,
      blocking: lila.core.pool.Blocking
  ): Seek = Seek(
    _id = makeId,
    variant = variant.id,
    daysPerTurn = daysPerTurn,
    mode = mode.id,
    user = LobbyUser.make(user, blocking),
    ratingRange = ratingRange.toString,
    createdAt = nowInstant
  )

  def renew(seek: Seek) = Seek(
    _id = makeId,
    variant = seek.variant,
    daysPerTurn = seek.daysPerTurn,
    mode = seek.mode,
    user = seek.user,
    ratingRange = seek.ratingRange,
    createdAt = nowInstant
  )

  import reactivemongo.api.bson.*
  import lila.db.dsl.{ *, given }
  given BSONHandler[LobbyPerf] = BSONIntegerHandler.as[LobbyPerf](
    b => LobbyPerf(IntRating(b.abs), RatingProvisional(b < 0)),
    x => x.rating.value * (if x.provisional.yes then -1 else 1)
  )
  private given BSONHandler[Map[PerfKey, LobbyPerf]]  = typedMapHandlerIso[PerfKey, LobbyPerf]
  private[lobby] given BSONDocumentHandler[LobbyUser] = Macros.handler
  private[lobby] given BSONDocumentHandler[Seek]      = Macros.handler
