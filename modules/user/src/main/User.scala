package lila.user

import scala.concurrent.duration._

import lila.common.{ LightUser, EmailAddress, NormalizedEmailAddress }

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
    title: Option[Title] = None,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    kid: Boolean,
    lang: Option[String],
    plan: Plan,
    reportban: Boolean = false,
    rankban: Boolean = false,
    totpSecret: Option[TotpSecret] = None
) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User => id == u.id
    case _ => false
  }

  override def hashCode: Int = id.hashCode

  override def toString =
    s"User $username(${perfs.bestRating}) games:${count.game}${troll ?? " troll"}${engine ?? " engine"}"

  def light = LightUser(id = id, name = username, title = title.map(_.value), isPatron = isPatron)

  def realNameOrUsername = profileOrDefault.nonEmptyRealName | username

  def langs = ("en" :: lang.toList).distinct.sorted

  def compare(other: User) = id compareTo other.id

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

  def watchList = booster || engine || troll || reportban || rankban || ipBan

  def lightPerf(key: String) = perfs(key) map { perf =>
    User.LightPerf(light, key, perf.intRating, perf.progress)
  }

  def lightCount = User.LightCount(light, count.game)

  private def bestOf(perfTypes: List[PerfType], nb: Int) =
    perfTypes.sortBy { pt =>
      -(perfs(pt).nb * PerfType.totalTimeRoughEstimation.get(pt).fold(0)(_.roundSeconds))
    } take nb

  def best8Perfs: List[PerfType] = bestOf(User.firstRow, 4) ::: bestOf(User.secondRow, 4)

  def best6Perfs: List[PerfType] = bestOf(User.firstRow ::: User.secondRow, 6)

  def hasEstablishedRating(pt: PerfType) = perfs(pt).established

  def isPatron = plan.active

  def activePlan: Option[Plan] = if (plan.active) Some(plan) else None

  def planMonths: Option[Int] = activePlan.map(_.months)

  def createdSinceDays(days: Int) = createdAt isBefore DateTime.now.minusDays(days)

  def is(name: String) = id == User.normalize(name)

  def isBot = title has Title.BOT
  def noBot = !isBot

  def rankable = noBot && !rankban

  def addRole(role: String) = copy(roles = role :: roles)
}

object User {

  type ID = String

  type CredentialCheck = ClearPassword => Boolean
  case class LoginCandidate(user: User, check: CredentialCheck) {
    import LoginCandidate._
    def apply(p: PasswordAndToken): Result = {
      val res =
        if (check(p.password)) user.totpSecret.fold[Result](Success(user)) { tp =>
          p.token.fold[Result](MissingTotpToken) { token =>
            if (tp verify token) Success(user) else InvalidTotpToken
          }
        }
        else InvalidUsernameOrPassword
      lila.mon.user.auth.result(res.success)()
      res
    }
    def option(p: PasswordAndToken): Option[User] = apply(p).toOption
  }
  object LoginCandidate {
    sealed abstract class Result(val toOption: Option[User]) {
      def success = toOption.isDefined
    }
    case class Success(user: User) extends Result(user.some)
    case object InvalidUsernameOrPassword extends Result(none)
    case object MissingTotpToken extends Result(none)
    case object InvalidTotpToken extends Result(none)
  }

  val anonymous = "Anonymous"
  val lichessId = "lichess"
  val broadcasterId = "broadcaster"
  def isOfficial(userId: ID) = userId == lichessId || userId == broadcasterId

  case class GDPRErase(user: User) extends AnyVal
  case class Erased(value: Boolean) extends AnyVal

  case class LightPerf(user: LightUser, perfKey: String, rating: Int, progress: Int)
  case class LightCount(user: LightUser, count: Int)

  case class Active(user: User)

  case class Emails(current: Option[EmailAddress], previous: Option[NormalizedEmailAddress]) {
    def list = current.toList ::: previous.toList
  }
  case class WithEmails(user: User, emails: Emails)

  case class ClearPassword(value: String) extends AnyVal {
    override def toString = "ClearPassword(****)"
  }
  case class TotpToken(value: String) extends AnyVal
  case class PasswordAndToken(password: ClearPassword, token: Option[TotpToken])

  case class Speaker(username: String, title: Option[Title], enabled: Boolean, troll: Option[Boolean]) {
    def isBot = title has Title.BOT
  }

  case class PlayTime(total: Int, tv: Int) {
    import org.joda.time.Period
    def totalPeriod = new Period(total * 1000l)
    def tvPeriod = new Period(tv * 1000l)
    def nonEmptyTvPeriod = (tv > 0) option tvPeriod
  }
  implicit def playTimeHandler = reactivemongo.bson.Macros.handler[PlayTime]

  // what existing usernames are like
  val historicalUsernameRegex = """(?i)[a-z0-9][\w-]{0,28}[a-z0-9]""".r

  // what new usernames should be like -- now split into further parts for clearer error messages
  val newUsernameRegex = """(?i)[a-z][\w-]{0,28}[a-z0-9]""".r

  val newUsernamePrefix = """(?i)[a-z].*""".r

  val newUsernameSuffix = """(?i).*[a-z0-9]""".r

  val newUsernameChars = """(?i)[\w-]*""".r

  def couldBeUsername(str: User.ID) = historicalUsernameRegex.matches(str)

  def normalize(username: String) = username.toLowerCase

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
    val verbatimEmail = "verbatimEmail"
    val mustConfirmEmail = "mustConfirmEmail"
    val prevEmail = "prevEmail"
    val colorIt = "colorIt"
    val plan = "plan"
    val reportban = "reportban"
    val rankban = "rankban"
    val salt = "salt"
    val bpass = "bpass"
    val sha512 = "sha512"
    val totpSecret = "totp"
    val watchList = "watchList"
    val changedCase = "changedCase"
  }

  import lila.db.BSON
  import lila.db.dsl._
  import Title.titleBsonHandler

  implicit val userBSONHandler = new BSON[User] {

    import BSONFields._
    import reactivemongo.bson.BSONDocument
    private implicit def countHandler = Count.countBSONHandler
    private implicit def profileHandler = Profile.profileBSONHandler
    private implicit def perfsHandler = Perfs.perfsBSONHandler
    private implicit def planHandler = Plan.planBSONHandler
    private implicit def totpSecretHandler = TotpSecret.totpSecretBSONHandler

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
      title = r.getO[Title](title),
      plan = r.getO[Plan](plan) | Plan.empty,
      reportban = r boolD reportban,
      rankban = r boolD rankban,
      totpSecret = r.getO[TotpSecret](totpSecret)
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
      rankban -> w.boolO(o.rankban),
      totpSecret -> o.totpSecret
    )
  }

  implicit val speakerHandler = reactivemongo.bson.Macros.handler[Speaker]

  private val firstRow: List[PerfType] = List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)
  private val secondRow: List[PerfType] = List(PerfType.UltraBullet, PerfType.Crazyhouse, PerfType.Chess960, PerfType.KingOfTheHill, PerfType.ThreeCheck, PerfType.Antichess, PerfType.Atomic, PerfType.Horde, PerfType.RacingKings)
}
