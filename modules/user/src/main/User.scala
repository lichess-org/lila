package lila.user

import scala.concurrent.duration._

import lila.common.{ LightUser, EmailAddress }

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
    playTime: Option[User.PlayTime],
    title: Option[String] = None,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    kid: Boolean,
    lang: Option[String],
    plan: Plan,
    reportban: Boolean = false,
    rankban: Boolean = false
) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User => id == u.id
    case _ => false
  }

  override def toString =
    s"User $username(${perfs.bestRating}) games:${count.game}${troll ?? " troll"}${engine ?? " engine"}"

  def light = LightUser(id = id, name = username, title = title, isPatron = isPatron)

  def realNameOrUsername = profileOrDefault.nonEmptyRealName | username

  def langs = ("en" :: lang.toList).distinct.sorted

  def compare(other: User) = id compare other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithBestRating = s"$username (${perfs.bestRating})"

  def titleUsername = title.fold(username)(t => s"$t $username")

  def titleUsernameWithBestRating = title.fold(usernameWithBestRating)(_ + " " + usernameWithBestRating)

  def profileOrDefault = profile | Profile.default

  def hasGames = count.game > 0

  def countRated = count.rated

  def hasTitle = title.isDefined

  lazy val seenRecently: Boolean = timeNoSee < 2.minutes

  def timeNoSee: Duration = seenAt.fold[Duration](Duration.Inf) { s =>
    (nowMillis - s.getMillis).millis
  }

  def everLoggedIn = seenAt.??(createdAt !=)

  def lame = booster || engine

  def lameOrTroll = lame || troll

  def lightPerf(key: String) = perfs(key) map { perf =>
    User.LightPerf(light, key, perf.intRating, perf.progress)
  }

  def lightCount = User.LightCount(light, count.game)

  private def bestOf(perfTypes: List[PerfType], nb: Int) =
    perfTypes.sortBy { pt =>
      -(perfs(pt).nb * PerfType.totalTimeRoughEstimation.get(pt).fold(0)(_.centis))
    } take nb

  private val firstRow: List[PerfType] = List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)
  private val secondRow: List[PerfType] = List(PerfType.UltraBullet, PerfType.Crazyhouse, PerfType.Chess960, PerfType.KingOfTheHill, PerfType.ThreeCheck, PerfType.Antichess, PerfType.Atomic, PerfType.Horde, PerfType.RacingKings)

  def best8Perfs: List[PerfType] = bestOf(firstRow, 4) ::: bestOf(secondRow, 4)

  def best6Perfs: List[PerfType] = bestOf(firstRow ::: secondRow, 6)

  def hasEstablishedRating(pt: PerfType) = perfs(pt).established

  def isPatron = plan.active

  def activePlan: Option[Plan] = if (plan.active) Some(plan) else None

  def planMonths: Option[Int] = activePlan.map(_.months)

  def createdSinceDays(days: Int) = createdAt isBefore DateTime.now.minusDays(days)

  def is(name: String) = id == User.normalize(name)

  def isBot = title has User.botTitle
  def noBot = !isBot

  def rankable = !isBot && !rankban
}

object User {

  type ID = String

  type CredentialCheck = ClearPassword => Boolean
  case class LoginCandidate(user: User, check: CredentialCheck) {
    def apply(p: ClearPassword): Option[User] = {
      val res = check(p)
      lila.mon.user.auth.result(res)()
      res option user
    }
  }

  val anonymous = "Anonymous"
  val lichessId = "lichess"

  val idPattern = """^[\w-]{3,20}$""".r.pattern

  case class LightPerf(user: LightUser, perfKey: String, rating: Int, progress: Int)
  case class LightCount(user: LightUser, count: Int)

  case class Active(user: User)

  case class Emails(current: Option[EmailAddress], previous: Option[EmailAddress])

  case class ClearPassword(value: String) extends AnyVal {
    override def toString = "ClearPassword(****)"
  }

  case class PlayTime(total: Int, tv: Int) {
    import org.joda.time.Period
    def totalPeriod = new Period(total * 1000l)
    def tvPeriod = new Period(tv * 1000l)
    def nonEmptyTvPeriod = (tv > 0) option tvPeriod
  }
  implicit def playTimeHandler = reactivemongo.bson.Macros.handler[PlayTime]

  // what existing usernames are like
  val historicalUsernameRegex = """(?i)[a-z0-9][\w-]*[a-z0-9]""".r
  // what new usernames should be like -- now split into further parts for clearer error messages
  val newUsernameRegex = """(?i)[a-z][\w-]*[a-z0-9]""".r

  val newUsernamePrefix = """(?i)[a-z].*""".r

  val newUsernameSuffix = """(?i).*[a-z0-9]""".r

  val newUsernameChars = """(?i)[\w-]*""".r

  def couldBeUsername(str: User.ID) = historicalUsernameRegex.pattern.matcher(str).matches && str.size < 30

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
    "LM" -> "Lichess Master",
    "BOT" -> "Chess Robot"
  )

  val botTitle = LightUser.botTitle

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
    val prevEmail = "prevEmail"
    val colorIt = "colorIt"
    val plan = "plan"
    val reportban = "reportban"
    val rankban = "rankban"
    val salt = "salt"
    val bpass = "bpass"
    val sha512 = "sha512"
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
      plan = r.getO[Plan](plan) | Plan.empty,
      reportban = r boolD reportban,
      rankban = r boolD rankban
    )

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
      plan -> o.plan.nonEmpty,
      reportban -> w.boolO(o.reportban),
      rankban -> w.boolO(o.rankban)
    )
  }
}
