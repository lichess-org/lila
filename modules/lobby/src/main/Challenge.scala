package lila.lobby

import chess.{ Mode, Clock, Speed }
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import actorApi.LobbyUser
import lila.game.PerfPicker
import lila.rating.RatingRange
import lila.user.{ User, Perfs }

// correspondence chess, persistent
case class Challenge(
    _id: String,
    variant: Int,
    daysPerTurn: Option[Int],
    mode: Int,
    color: String,
    user: LobbyUser,
    destUserId: String,
    createdAt: DateTime) {

  def id = _id

  val realColor = Color orDefault color

  val realVariant = chess.variant.Variant orDefault variant

  val realMode = Mode orDefault mode

  def rating = perfType map (_.key) flatMap user.ratingMap.get

  def render: JsObject = Json.obj(
    "id" -> _id,
    "username" -> user.username,
    "rating" -> rating,
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

  val idSize = 8

  def make(
    variant: chess.variant.Variant,
    daysPerTurn: Option[Int],
    mode: Mode,
    color: String,
    user: User,
    destUserId: String,
    ratingRange: RatingRange): Seek = new Seek(
    _id = Random nextStringUppercase idSize,
    variant = variant.id,
    daysPerTurn = daysPerTurn,
    mode = mode.id,
    color = color,
    user = LobbyUser.make(user, Set.empty),
    destUserId = destUserId,
    createdAt = DateTime.now)

  import reactivemongo.bson.Macros
  import lila.db.BSON.MapValue.MapHandler
  import lila.db.BSON.BSONJodaDateTimeHandler
  private[lobby] implicit val lobbyUserBSONHandler = Macros.handler[LobbyUser]
  private[lobby] implicit val challengeBSONHandler = Macros.handler[Challenge]
}
