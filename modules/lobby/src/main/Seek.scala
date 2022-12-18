package lila.lobby

import chess.{ Mode, Speed }
import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.libs.json.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.Days
import lila.common.Json.given
import lila.game.PerfPicker
import lila.rating.{ Perf, RatingRange }
import lila.user.User

// correspondence chess, persistent
case class Seek(
    _id: String,
    variant: Int,
    daysPerTurn: Option[Days],
    mode: Int,
    color: String,
    user: LobbyUser,
    ratingRange: String,
    createdAt: DateTime
):

  inline def id = _id

  val realColor = Color orDefault color

  val realVariant = chess.variant.Variant orDefault variant

  val realMode = Mode orDefault mode

  def compatibleWith(h: Seek) =
    user.id != h.user.id &&
      compatibilityProperties == h.compatibilityProperties &&
      (realColor compatibleWith h.realColor) &&
      ratingRangeCompatibleWith(h) && h.ratingRangeCompatibleWith(this)

  private def ratingRangeCompatibleWith(s: Seek) =
    realRatingRange.fold(true) { range =>
      s.rating ?? range.contains
    }

  private def compatibilityProperties = (variant, mode, daysPerTurn)

  lazy val realRatingRange: Option[RatingRange] = RatingRange noneIfDefault ratingRange

  def perf = perfType map user.perfAt

  def rating = perf.map(_.rating)

  def render: JsObject =
    Json
      .obj(
        "id"       -> _id,
        "username" -> user.username,
        "rating"   -> rating,
        "variant"  -> Json.obj("key" -> realVariant.key),
        "mode"     -> realMode.id,
        "color"    -> (chess.Color.fromName(color).??(_.name): String)
      )
      .add("days" -> daysPerTurn)
      .add("perf" -> perfType.map { pt =>
        Json.obj("key" -> pt.key)
      })
      .add("provisional" -> perf.exists(_.provisional.yes))

  lazy val perfType = PerfPicker.perfType(Speed.Correspondence, realVariant, daysPerTurn)

object Seek:

  val idSize = 8

  def make(
      variant: chess.variant.Variant,
      daysPerTurn: Option[Days],
      mode: Mode,
      color: String,
      user: User,
      ratingRange: RatingRange,
      blocking: lila.pool.Blocking
  ): Seek = Seek(
    _id = ThreadLocalRandom nextString idSize,
    variant = variant.id,
    daysPerTurn = daysPerTurn,
    mode = mode.id,
    color = color,
    user = LobbyUser.make(user, blocking),
    ratingRange = ratingRange.toString,
    createdAt = DateTime.now
  )

  def renew(seek: Seek) = Seek(
    _id = ThreadLocalRandom nextString idSize,
    variant = seek.variant,
    daysPerTurn = seek.daysPerTurn,
    mode = seek.mode,
    color = seek.color,
    user = seek.user,
    ratingRange = seek.ratingRange,
    createdAt = DateTime.now
  )

  import reactivemongo.api.bson.*
  import lila.db.dsl.{ *, given }
  given BSONHandler[LobbyPerf] = BSONIntegerHandler.as[LobbyPerf](
    b => LobbyPerf(IntRating(b.abs), RatingProvisional(b < 0)),
    x => x.rating.value * (if x.provisional.yes then -1 else 1)
  )
  private given BSONHandler[Map[Perf.Key, LobbyPerf]] = typedMapHandler[Perf.Key, LobbyPerf]
  private[lobby] given BSONDocumentHandler[LobbyUser] = Macros.handler
  private[lobby] given BSONDocumentHandler[Seek]      = Macros.handler
