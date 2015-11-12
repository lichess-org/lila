package lila.setup

import chess.{ Mode, Clock, Speed }
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import lila.game.PerfPicker
import lila.lobby.Color
import lila.rating.{ PerfType, RatingRange }
import lila.user.{ User, Perfs }

// correspondence chess, persistent
case class Challenge(
    _id: String,
    variant: Int,
    daysPerTurn: Option[Int],
    mode: Int,
    color: String,
    challenger: Challenge.Challenger,
    destUserId: String,
    createdAt: DateTime) {

  def id = _id

  val realColor = Color orDefault color

  val realVariant = chess.variant.Variant orDefault variant

  val realMode = Mode orDefault mode

  def render: JsObject = Json.obj(
    "id" -> _id,
    "challenger" -> Json.obj(
      "id" -> challenger.id,
      "username" -> challenger.username,
      "rating" -> challenger.rating
    ),
    "destUserId" -> destUserId,
    "variant" -> Json.obj(
      "key" -> realVariant.key,
      "short" -> realVariant.shortName,
      "name" -> realVariant.name),
    "mode" -> realMode.id,
    "days" -> daysPerTurn,
    "color" -> chess.Color(color).??(_.name),
    "perf" -> Json.obj(
      "icon" -> perfType.map(_.iconChar.toString),
      "name" -> perfType.map(_.name))
  )

  lazy val perfType = PerfPicker.perfType(Speed.Correspondence, realVariant, daysPerTurn)
}

object Challenge {

  private[setup] case class Challenger(id: String, username: String, rating: Int)

  val idSize = 8

  def make(
    variant: chess.variant.Variant,
    daysPerTurn: Option[Int],
    mode: Mode,
    color: String,
    user: User,
    destUserId: String,
    ratingRange: RatingRange): Challenge = new Challenge(
    _id = Random nextStringUppercase idSize,
    variant = variant.id,
    daysPerTurn = daysPerTurn,
    mode = mode.id,
    color = color,
    challenger = Challenger(user.id, user.username, user.perfs {
      PerfPicker.perfType(Speed.Correspondence, variant, daysPerTurn) | PerfType.Correspondence
    }.intRating),
    destUserId = destUserId,
    createdAt = DateTime.now)

  import reactivemongo.bson.Macros
  import lila.db.BSON.BSONJodaDateTimeHandler
  private[setup] implicit val challengerBSONHandler = Macros.handler[Challenger]
  private[setup] implicit val challengeBSONHandler = Macros.handler[Challenge]
}
