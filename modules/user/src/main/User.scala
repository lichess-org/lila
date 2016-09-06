package lila.user

import scala.concurrent.duration._

import lila.common.LightUser

import chess.Speed
import lila.rating.PerfType
import org.joda.time.DateTime

case class User(
    id: String,
    username: String,
    perfs: Perfs,
    count: Count,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    profile: Option[Profile] = None,
    engine: Boolean = false,
    booster: Boolean = false,
    toints: Int = 0,
    playTime: Option[User.PlayTime] = None,
    title: Option[String] = None,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    kid: Boolean,
    lang: Option[String],
    plan: Plan) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User => id == u.id
    case _       => false
  }

  override def toString =
    s"User $username(${perfs.bestRating}) games:${count.game}${troll ?? " troll"}${engine ?? " engine"}"

  def light = LightUser(id = id, name = username, title = title, isPatron = isPatron)

  def titleName = title.fold(username)(_ + " " + username)

  def realNameOrUsername = profileOrDefault.nonEmptyRealName | username

  def langs = ("en" :: lang.toList).distinct.sorted

  def compare(other: User) = id compare other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithBestRating = s"$username (${perfs.bestRating})"

  def titleUsername = title.fold(username)(_ + " " + username)

  def titleUsernameWithBestRating = title.fold(usernameWithBestRating)(_ + " " + usernameWithBestRating)

  def profileOrDefault = profile | Profile.default

  def hasGames = count.game > 0

  def countRated = count.rated

  def hasTitle = title.isDefined

  lazy val seenRecently: Boolean = timeNoSee < 2.minutes

  def timeNoSee: Duration = seenAt.fold[Duration](Duration.Inf) { s =>
    (nowMillis - s.getMillis).millis
  }

  def lame = booster || engine

  def lameOrTroll = lame || troll

  def lightPerf(key: String) = perfs(key) map { perf =>
    User.LightPerf(light, key, perf.intRating, perf.progress)
  }

  def lightCount = User.LightCount(light, count.game)

  private def best4Of(perfTypes: List[PerfType]) =
    perfTypes.sortBy { pt => -perfs(pt).nb } take 4

  def best8Perfs: List[PerfType] =
    best4Of(List(PerfType.Bullet, PerfType.Blitz, PerfType.Classical, PerfType.Correspondence)) :::
      best4Of(List(PerfType.Crazyhouse, PerfType.Chess960, PerfType.KingOfTheHill, PerfType.ThreeCheck, PerfType.Antichess, PerfType.Atomic, PerfType.Horde, PerfType.RacingKings))

  def hasEstablishedRating(pt: PerfType) = perfs(pt).established

  def isPatron = plan.active

  def activePlan: Option[Plan] = if (plan.active) Some(plan) else None

  def planMonths: Option[Int] = activePlan.map(_.months)
}

object User {

  type ID = String

  type CredentialCheck = String => Boolean
  case class LoginCandidate(user: User, check: CredentialCheck) {
    def apply(password: String): Option[User] = check(password) option user
  }

  val anonymous = "Anonymous"

  case class LightPerf(user: LightUser, perfKey: String, rating: Int, progress: Int)
  case class LightCount(user: LightUser, count: Int)

  case class Active(user: User)

  case class PlayTime(total: Int, tv: Int) {
    import org.joda.time.Period
    def totalPeriod = new Period(total * 1000l)
    def tvPeriod = (tv > 0) option new Period(tv * 1000l)
  }
  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit def playTimeHandler = reactivemongo.bson.Macros.handler[PlayTime]

  // Matches a lichess username with a '@' prefix only if the next char isn't a digit,
  // if it isn't after a word character (that'd be an email) and fits constraints in
  // https://github.com/ornicar/lila/blob/master/modules/security/src/main/DataForm.scala#L34-L44
  // Example: everyone says @ornicar is a pretty cool guy
  // False example: Write to contact@lichess.org, @1
  val atUsernameRegex = """\B@(?>([a-zA-Z_-][\w-]{1,19}))(?U)(?![\w-])""".r

  val usernameRegex = """^[\w-]+$""".r
  def couldBeUsername(str: String) = usernameRegex.pattern.matcher(str).matches

  def normalize(username: String) = username.toLowerCase

  val titles = Seq(
    "GM" -> "Grandmaster",
    "WGM" -> "Woman Grandmaster",
    "IM" -> "International Master",
    "WIM" -> "Woman Intl. Master",
    "FM" -> "FIDE Master",
    "WFM" -> "Woman FIDE Master",
    "NM" -> "National Master",
    "CM" -> "Candidate Master",
    "WCM" -> "Woman Candidate Master",
    "WNM" -> "Woman National Master",
    "LM" -> "Lichess Master")

  val titlesMap = titles.toMap

  def titleName(title: String) = titlesMap get title getOrElse title

  object BSONFields {
    val id = "_id"
    val username = "username"
    val perfs = "perfs"
    val count = "count"
    val troll = "troll"
    val ipBan = "ipBan"
    val enabled = "enabled"
    val roles = "roles"
    val profile = "profile"
    val engine = "engine"
    val booster = "booster"
    val toints = "toints"
    val playTime = "time"
    val createdAt = "createdAt"
    val seenAt = "seenAt"
    val kid = "kid"
    val createdWithApiVersion = "createdWithApiVersion"
    val lang = "lang"
    val title = "title"
    def glicko(perf: String) = s"$perfs.$perf.gl"
    val email = "email"
    val mustConfirmEmail = "mustConfirmEmail"
    val colorIt = "colorIt"
    val plan = "plan"
  }

  import lila.db.BSON
  import lila.db.dsl._

  implicit val userBSONHandler = new BSON[User] {

    import BSONFields._
    import reactivemongo.bson.BSONDocument
    private implicit def countHandler = Count.countBSONHandler
    private implicit def profileHandler = Profile.profileBSONHandler
    private implicit def perfsHandler = Perfs.perfsBSONHandler
    private implicit def planHandler = Plan.planBSONHandler

    def reads(r: BSON.Reader): User = User(
      id = r str id,
      username = r str username,
      perfs = r.getO[Perfs](perfs) | Perfs.default,
      count = r.get[Count](count),
      troll = r boolD troll,
      ipBan = r boolD ipBan,
      enabled = r bool enabled,
      roles = ~r.getO[List[String]](roles),
      profile = r.getO[Profile](profile),
      engine = r boolD engine,
      booster = r boolD booster,
      toints = r nIntD toints,
      playTime = r.getO[PlayTime](playTime),
      createdAt = r date createdAt,
      seenAt = r dateO seenAt,
      kid = r boolD kid,
      lang = r strO lang,
      title = r strO title,
      plan = r.getO[Plan](plan) | Plan.empty)

    def writes(w: BSON.Writer, o: User) = BSONDocument(
      id -> o.id,
      username -> o.username,
      perfs -> o.perfs,
      count -> o.count,
      troll -> w.boolO(o.troll),
      ipBan -> w.boolO(o.ipBan),
      enabled -> o.enabled,
      roles -> o.roles.some.filter(_.nonEmpty),
      profile -> o.profile,
      engine -> w.boolO(o.engine),
      booster -> w.boolO(o.booster),
      toints -> w.intO(o.toints),
      playTime -> o.playTime,
      createdAt -> o.createdAt,
      seenAt -> o.seenAt,
      kid -> w.boolO(o.kid),
      lang -> o.lang,
      title -> o.title,
      plan -> o.plan.nonEmpty)
  }
}
