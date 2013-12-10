package lila.user

import scala.concurrent.duration._

import chess.Speed
import org.joda.time.DateTime

case class User(
    id: String,
    username: String,
    elo: Int,
    speedElos: SpeedElos,
    variantElos: VariantElos,
    count: Count,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    profile: Option[Profile] = None,
    engine: Boolean = false,
    toints: Int = 0,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    lang: Option[String]) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User ⇒ id == u.id
    case _       ⇒ false
  }

  def compare(other: User) = id compare other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithElo = "%s (%d)".format(username, elo)

  def profileOrDefault = profile | Profile.default

  def hasGames = count.game > 0

  def countRated = count.rated

  private val recentDuration = 10.minutes
  def seenRecently: Boolean = timeNoSee < recentDuration

  def timeNoSee: Duration = seenAt.fold[Duration](Duration.Inf) { s ⇒
    (nowMillis - s.getMillis).millis
  }
}

object User {

  type ID = String

  val STARTING_ELO = 1200

  val anonymous = "Anonymous"

  case class Active(user: User, lang: String)

  object BSONFields {
    val id = "_id"
    val username = "username"
    val elo = "elo"
    val speedElos = "speedElos"
    val variantElos = "variantElos"
    val count = "count"
    val troll = "troll"
    val ipBan = "ipBan"
    val enabled = "enabled"
    val roles = "roles"
    val profile = "profile"
    val engine = "engine"
    val toints = "toints"
    val createdAt = "createdAt"
    val seenAt = "seenAt"
    val lang = "lang"
  }

  import reactivemongo.bson.{ Macros, BSONDocument }
  import lila.db.BSON
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit def countBsTube = Count.bsTube.handler
  private implicit def speedElosBsTube = SpeedElos.bsTube.handler
  private implicit def variantElosBsTube = VariantElos.bsTube.handler
  private implicit def profileBsTube = Profile.bsTube.handler
  private[user] lazy val bsTube = lila.db.BsTube(Macros.handler[User])

  implicit val userBSONHandler = new BSON[User] {

    import BSONFields._

    def reads(r: BSON.Reader): User = User(
      id = r str id,
      username = r str username,
      elo = r int elo,
      speedElos = r.getO[SpeedElos](speedElos) | SpeedElos.default,
      variantElos = r.getO[VariantElos](variantElos) | VariantElos.default,
      count = r.get[Count](count),
      troll = r boolD troll,
      ipBan = r boolD ipBan,
      enabled = r bool enabled,
      roles = ~r.getO[List[String]](roles),
      profile = r.getO[Profile](profile),
      engine = r boolD engine,
      toints = r intD toints,
      createdAt = r date createdAt,
      seenAt = r dateO seenAt,
      lang = r strO lang)

    def writes(w: BSON.Writer, o: User) = BSONDocument(
      id -> o.id,
    username -> o.username,
    elo -> o.elo,
    speedElos -> o.speedElos,
    variantElos -> o.variantElos,
    count -> o.count,
    troll -> w.boolO(o.troll),
    ipBan -> w.boolO(o.ipBan),
    enabled -> o.enabled,
    roles -> o.roles.some.filter(_.nonEmpty),
    profile -> o.profile,
    engine -> w.boolO(o.engine),
    toints -> w.intO(o.toints),
    createdAt -> o.createdAt,
    seenAt -> o.seenAt,
    lang -> o.lang)
  }

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private implicit def countTube = Count.tube
  private implicit def speedElosTube = SpeedElos.tube
  private implicit def variantElosTube = VariantElos.tube
  private implicit def profileTube = Profile.tube

  private[user] lazy val tube = JsTube[User](
    (__.json update (
      merge(defaults) andThen readDate('createdAt) andThen readDateOpt('seenAt)
    )) andThen Json.reads[User],
    Json.writes[User] andThen (__.json update writeDate('createdAt)) andThen (__.json update writeDateOpt('seenAt))
  )

  def normalize(username: String) = username.toLowerCase

  private def defaults = Json.obj(
    "speedElos" -> SpeedElos.default,
    "variantElos" -> VariantElos.default,
    "troll" -> false,
    "ipBan" -> false,
    "engine" -> false,
    "toints" -> 0,
    "roles" -> Json.arr(),
    "seenAt" -> none[DateTime],
    "lang" -> none[String])
}
