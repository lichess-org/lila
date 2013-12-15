package lila.user

import scala.concurrent.duration._

import chess.Speed
import org.joda.time.DateTime

case class User(
    id: String,
    username: String,
    elo: Int,
    glicko: Glicko,
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

  def normalize(username: String) = username.toLowerCase

  object BSONFields {
    val id = "_id"
    val username = "username"
    val elo = "elo"
    val glicko = "glicko"
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

  import lila.db.BSON
  import lila.db.BSON.BSONJodaDateTimeHandler

  private def userBSONHandler = new BSON[User] {

    import BSONFields._
    import reactivemongo.bson.BSONDocument
    implicit def countHandler = Count.tube.handler
    implicit def speedElosHandler = SpeedElos.tube.handler
    implicit def variantElosHandler = VariantElos.tube.handler
    implicit def profileHandler = Profile.tube.handler
    implicit def glickoHandler = Glicko.tube.handler

    def reads(r: BSON.Reader): User = User(
      id = r str id,
      username = r str username,
      elo = r nInt elo,
      glicko = r.getO[Glicko](glicko) | Glicko.default,
      speedElos = r.getO[SpeedElos](speedElos) | SpeedElos.default,
      variantElos = r.getO[VariantElos](variantElos) | VariantElos.default,
      count = r.get[Count](count),
      troll = r boolD troll,
      ipBan = r boolD ipBan,
      enabled = r bool enabled,
      roles = ~r.getO[List[String]](roles),
      profile = r.getO[Profile](profile),
      engine = r boolD engine,
      toints = r nIntD toints,
      createdAt = r date createdAt,
      seenAt = r dateO seenAt,
      lang = r strO lang)

    def writes(w: BSON.Writer, o: User) = BSONDocument(
      id -> o.id,
      username -> o.username,
      elo -> w.int(o.elo),
      glicko -> o.glicko,
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

  private[user] lazy val tube = lila.db.BsTube(userBSONHandler)
}
