package lila.lobby

import chess.{ Mode, Speed }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.i18n.Lang

import lila.game.PerfPicker
import lila.rating.RatingRange
import lila.user.User

// correspondence chess, persistent
case class Seek(
    _id: String,
    variant: Int,
    daysPerTurn: Option[Int],
    mode: Int,
    color: String,
    user: LobbyUser,
    ratingRange: String,
    createdAt: DateTime
) {

  def id = _id

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
        "mode"     -> realMode.id,
        "color"    -> chess.Color.fromName(color).??(_.name)
      )
      .add("days" -> daysPerTurn)
      .add("perf" -> perfType.map { pt =>
        Json.obj("key" -> pt.key)
      })
      .add("provisional" -> perf.exists(_.provisional))

  lazy val perfType = PerfPicker.perfType(Speed.Correspondence, realVariant, daysPerTurn)
}

object Seek {

  val idSize = 8

  def make(
      variant: chess.variant.Variant,
      daysPerTurn: Option[Int],
      mode: Mode,
      color: String,
      user: User,
      ratingRange: RatingRange,
      blocking: Set[String]
  ): Seek =
    new Seek(
      _id = lila.common.ThreadLocalRandom nextString idSize,
      variant = variant.id,
      daysPerTurn = daysPerTurn,
      mode = mode.id,
      color = color,
      user = LobbyUser.make(user, blocking),
      ratingRange = ratingRange.toString,
      createdAt = DateTime.now
    )

  def renew(seek: Seek) =
    new Seek(
      _id = lila.common.ThreadLocalRandom nextString idSize,
      variant = seek.variant,
      daysPerTurn = seek.daysPerTurn,
      mode = seek.mode,
      color = seek.color,
      user = seek.user,
      ratingRange = seek.ratingRange,
      createdAt = DateTime.now
    )

  import reactivemongo.api.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit val lobbyPerfBSONHandler =
    BSONIntegerHandler.as[LobbyPerf](
      b => LobbyPerf(b.abs, b < 0),
      x => x.rating * (if (x.provisional) -1 else 1)
    )
  implicit private[lobby] val lobbyUserBSONHandler = Macros.handler[LobbyUser]
  implicit private[lobby] val seekBSONHandler      = Macros.handler[Seek]
}
