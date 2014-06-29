package lila.user

import scala.concurrent.duration._

import chess.Speed
import org.joda.time.DateTime

case class User(
    id: String,
    username: String,
    rating: Int,
    progress: Int,
    perfs: Perfs,
    count: Count,
    artificial: Boolean = false,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    profile: Option[Profile] = None,
    engine: Boolean = false,
    toints: Int = 0,
    playTime: Option[User.PlayTime] = None,
    title: Option[String] = None,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    lang: Option[String]) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User => id == u.id
    case _       => false
  }

  override def toString =
    s"User $username($rating) games:${count.game}${troll ?? " troll"}${engine ?? " engine"}"

  def light = lila.common.LightUser(id = id, name = username, title = title)

  def langs = ("en" :: lang.toList).distinct.sorted

  def compare(other: User) = id compare other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithRating = s"$username ($rating)"

  def titleUsername = title.fold(username)(_ + " " + username)

  def titleUsernameWithRating = title.fold(usernameWithRating)(_ + " " + usernameWithRating)

  def profileOrDefault = profile | Profile.default

  def hasGames = count.game > 0

  def countRated = count.rated

  def hasTitle = title.isDefined

  private val recentDuration = 10.minutes
  def seenRecently: Boolean = timeNoSee < recentDuration

  def timeNoSee: Duration = seenAt.fold[Duration](Duration.Inf) { s =>
    (nowMillis - s.getMillis).millis
  }
}

object User {

  type ID = String

  val anonymous = "Anonymous"

  case class Active(user: User, lang: String)

  case class PlayTime(total: Int, tv: Int) {
    import org.joda.time.Period
    def totalPeriod = new Period(total * 1000l)
    def tvPeriod = (tv > 0) option new Period(tv * 1000l)
  }
  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit def playTimeHandler = reactivemongo.bson.Macros.handler[PlayTime]

  def normalize(username: String) = username.toLowerCase

  val titles = Seq(
    "GM" -> "Grandmaster",
    "WGM" -> "Woman Grandmaster",
    "IM" -> "International Master",
    "WIM" -> "Woman Intl. Master",
    "FM" -> "FIDE Master",
    "NM" -> "National Master",
    "CM" -> "FIDE Candidate Master",
    "WCM" -> "FIDE Woman Candidate Master",
    "WNM" -> "Woman National Master",
    "LM" -> "Lichess Master")

  val titlesMap = titles.toMap

  def titleName(title: String) = titlesMap get title getOrElse title

  object BSONFields {
    val id = "_id"
    val username = "username"
    val rating = "rating"
    val progress = "progress"
    val artificial = "artificial"
    val perfs = "perfs"
    val count = "count"
    val troll = "troll"
    val ipBan = "ipBan"
    val enabled = "enabled"
    val roles = "roles"
    val profile = "profile"
    val engine = "engine"
    val toints = "toints"
    val playTime = "time"
    val createdAt = "createdAt"
    val seenAt = "seenAt"
    val lang = "lang"
    val title = "title"
    def glicko(perf: String) = s"$perfs.$perf.gl"
  }

  import lila.db.BSON

  private def userBSONHandler = new BSON[User] {

    import BSONFields._
    import reactivemongo.bson.BSONDocument
    private implicit def countHandler = Count.tube.handler
    private implicit def profileHandler = Profile.tube.handler
    private implicit def perfsHandler = Perfs.tube.handler

    def reads(r: BSON.Reader): User = User(
      id = r str id,
      username = r str username,
      rating = r nInt rating,
      progress = r intD progress,
      perfs = r.getO[Perfs](perfs) | Perfs.default,
      count = r.get[Count](count),
      artificial = r boolD artificial,
      troll = r boolD troll,
      ipBan = r boolD ipBan,
      enabled = r bool enabled,
      roles = ~r.getO[List[String]](roles),
      profile = r.getO[Profile](profile),
      engine = r boolD engine,
      toints = r nIntD toints,
      playTime = r.getO[PlayTime](playTime),
      createdAt = r date createdAt,
      seenAt = r dateO seenAt,
      lang = r strO lang,
      title = r strO title)

    def writes(w: BSON.Writer, o: User) = BSONDocument(
      id -> o.id,
      username -> o.username,
      rating -> w.int(o.rating),
      progress -> w.int(o.progress),
      perfs -> o.perfs,
      count -> o.count,
      artificial -> w.boolO(o.artificial),
      troll -> w.boolO(o.troll),
      ipBan -> w.boolO(o.ipBan),
      enabled -> o.enabled,
      roles -> o.roles.some.filter(_.nonEmpty),
      profile -> o.profile,
      engine -> w.boolO(o.engine),
      toints -> w.intO(o.toints),
      playTime -> o.playTime,
      createdAt -> o.createdAt,
      seenAt -> o.seenAt,
      lang -> o.lang,
      title -> o.title)
  }

  private[user] lazy val tube = lila.db.BsTube(userBSONHandler)
}
