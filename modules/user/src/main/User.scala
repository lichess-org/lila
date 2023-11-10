package lila.user

import play.api.i18n.Lang

import lila.common.{ EmailAddress, LightUser, NormalizedEmailAddress }
import lila.rating.{ Perf, PerfType }
import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler, Macros }

case class User(
    id: UserId,
    username: UserName,
    count: Count,
    enabled: UserEnabled,
    roles: List[String],
    profile: Option[Profile] = None,
    toints: Int = 0,
    playTime: Option[User.PlayTime],
    title: Option[UserTitle] = None,
    createdAt: Instant,
    seenAt: Option[Instant],
    kid: Boolean,
    lang: Option[String],
    plan: Plan,
    totpSecret: Option[TotpSecret] = None,
    marks: UserMarks = UserMarks.empty
):

  override def equals(other: Any) = other match
    case u: User => id == u.id
    case _       => false

  override def hashCode: Int = id.hashCode

  override def toString =
    s"User $username games:${count.game}${marks.troll so " troll"}${marks.engine so " engine"}${enabled.no so " closed"}"

  def light = LightUser(id = id, name = username, title = title, isPatron = isPatron)

  def realNameOrUsername = profileOrDefault.nonEmptyRealName | username.value

  def realLang = lang flatMap Lang.get

  def titleUsername: String = title.fold(username.value)(t => s"$t $username")

  def profileOrDefault = profile | Profile.default

  def hasGames = count.game > 0

  def countRated = count.rated

  def hasTitle = title.exists(Title.BOT !=)

  lazy val seenRecently: Boolean = timeNoSee < User.seenRecently

  def timeNoSee: Duration = (nowMillis - (seenAt | createdAt).toMillis).millis

  def everLoggedIn = seenAt.so(createdAt !=)

  def lame = marks.boost || marks.engine

  def lameOrTroll      = lame || marks.troll
  def lameOrAlt        = lame || marks.alt
  def lameOrTrollOrAlt = lameOrTroll || marks.alt

  def canBeFeatured = hasTitle && !lameOrTroll

  def canFullyLogin = enabled.yes || !lameOrTrollOrAlt

  def withMarks(f: UserMarks => UserMarks) = copy(marks = f(marks))

  def lightCount = User.LightCount(light, count.game)

  def isPatron = plan.active

  def activePlan: Option[Plan] = plan.active option plan

  def planMonths: Option[Int] = activePlan.map(_.months)

  def mapPlan(f: Plan => Plan) = copy(plan = f(plan))

  def createdSinceDays(days: Int) = createdAt isBefore nowInstant.minusDays(days)

  def isBot = title has Title.BOT
  def noBot = !isBot

  def rankable = enabled.yes && noBot && !marks.rankban

  def withPerf(perf: Perf): User.WithPerf = User.WithPerf(this, perf)

  def addRole(role: String) = copy(roles = role :: roles)

  def isVerified        = roles.exists(_ contains "ROLE_VERIFIED")
  def isSuperAdmin      = roles.exists(_ contains "ROLE_SUPER_ADMIN")
  def isAdmin           = roles.exists(_ contains "ROLE_ADMIN") || isSuperAdmin
  def isApiHog          = roles.exists(_ contains "ROLE_API_HOG")
  def isVerifiedOrAdmin = isVerified || isAdmin

  def has2fa = totpSecret.isDefined

object User:

  given UserIdOf[User] = _.id

  export lila.user.UserEnabled as Enabled

  case class WithPerfs(user: User, perfs: UserPerfs):
    export user.*
    def usernameWithBestRating = s"$username (${perfs.bestRating})"
    def hasVariantRating       = PerfType.variants.exists(perfs.apply(_).nonEmpty)
    def titleUsernameWithBestRating =
      title.fold(usernameWithBestRating): t =>
        s"$t $usernameWithBestRating"
    def lightPerf(key: Perf.Key) =
      perfs(key).map: perf =>
        User.LightPerf(light, key, perf.intRating, perf.progress)

    def only(pt: PerfType) = WithPerf(user, perfs(pt))

  object WithPerfs:
    def apply(user: User, perfs: Option[UserPerfs]): WithPerfs =
      new WithPerfs(user, perfs | UserPerfs.default(user.id))
    given UserIdOf[WithPerfs] = _.user.id

  case class WithPerf(user: User, perf: Perf):
    export user.{ id, createdAt, hasTitle, light }

  type CredentialCheck = ClearPassword => Boolean
  case class LoginCandidate(user: User, check: CredentialCheck, isBlanked: Boolean, must2fa: Boolean = false):
    import LoginCandidate.*
    def apply(p: PasswordAndToken): Result =
      val res =
        if !user.has2fa && must2fa then Result.Must2fa
        else if check(p.password) then
          user.totpSecret.fold[Result](Result.Success(user)): tp =>
            p.token.fold[Result](Result.MissingTotpToken): token =>
              if tp verify token then Result.Success(user) else Result.InvalidTotpToken
        else if isBlanked then Result.BlankedPassword
        else Result.InvalidUsernameOrPassword
      lila.mon.user.auth.count(res.success).increment()
      res
    def option(p: PasswordAndToken): Option[User] = apply(p).toOption
  object LoginCandidate:
    enum Result(val toOption: Option[User]):
      def success = toOption.isDefined
      case Success(user: User)       extends Result(user.some)
      case InvalidUsernameOrPassword extends Result(none)
      case Must2fa                   extends Result(none)
      case BlankedPassword           extends Result(none)
      case WeakPassword              extends Result(none)
      case MissingTotpToken          extends Result(none)
      case InvalidTotpToken          extends Result(none)

  val anonymous: UserName              = UserName("Anonymous")
  val anonMod: String                  = "A Lichess Moderator"
  val lichessName: UserName            = UserName("lichess")
  val lichessId: UserId                = lichessName.id
  val lichessIdAsMe: Me.Id             = lichessId.into(Me.Id)
  val broadcasterId                    = UserId("broadcaster")
  val irwinId                          = UserId("irwin")
  val kaladinId                        = UserId("kaladin")
  val explorerId                       = UserId("openingexplorer")
  val lichess4545Id                    = UserId("lichess4545")
  val challengermodeId                 = UserId("challengermode")
  val watcherbotId                     = UserId("watcherbot")
  val ghostId                          = UserId("ghost")
  def isLichess[U: UserIdOf](user: U)  = lichessId is user
  def isOfficial[U: UserIdOf](user: U) = isLichess(user) || broadcasterId.is(user)

  val seenRecently = 2.minutes

  case class GDPRErase(user: User) extends AnyVal
  opaque type Erased = Boolean
  object Erased extends YesNo[Erased]

  case class LightPerf(user: LightUser, perfKey: Perf.Key, rating: IntRating, progress: IntRatingDiff)
  case class LightCount(user: LightUser, count: Int)

  case class Emails(current: Option[EmailAddress], previous: Option[NormalizedEmailAddress]):
    def strList = current.map(_.value).toList ::: previous.map(_.value).toList

  case class WithEmails(user: User.WithPerfs, emails: Emails)

  case class ClearPassword(value: String) extends AnyVal:
    override def toString = "ClearPassword(****)"

  case class TotpToken(value: String) extends AnyVal
  case class PasswordAndToken(password: ClearPassword, token: Option[TotpToken])

  case class Speaker(
      username: UserName,
      title: Option[UserTitle],
      enabled: Boolean,
      plan: Option[Plan],
      marks: Option[UserMarks]
  ):
    def isBot    = title has Title.BOT
    def isTroll  = marks.exists(_.troll)
    def isPatron = plan.exists(_.active)

  case class Contact(
      _id: UserId,
      kid: Option[Boolean],
      marks: Option[UserMarks],
      roles: Option[List[String]],
      createdAt: Instant
  ):
    def id                     = _id
    def isKid                  = ~kid
    def isTroll                = marks.exists(_.troll)
    def isVerified             = roles.exists(_ contains "ROLE_VERIFIED")
    def isApiHog               = roles.exists(_ contains "ROLE_API_HOG")
    def isDaysOld(days: Int)   = createdAt isBefore nowInstant.minusDays(days)
    def isHoursOld(hours: Int) = createdAt isBefore nowInstant.minusHours(hours)
    def isLichess              = _id == User.lichessId
  case class Contacts(orig: Contact, dest: Contact):
    def hasKid  = orig.isKid || dest.isKid
    def userIds = List(orig.id, dest.id)

  case class PlayTime(total: Int, tv: Int):
    import java.time.Duration
    def totalDuration      = Duration.ofSeconds(total)
    def tvDuration         = Duration.ofSeconds(tv)
    def nonEmptyTvDuration = tv > 0 option tvDuration
  given BSONDocumentHandler[PlayTime] = Macros.handler[PlayTime]

  // what existing usernames are like
  val historicalUsernameRegex = "(?i)[a-z0-9][a-z0-9_-]{0,28}[a-z0-9]".r
  // what new usernames should be like -- now split into further parts for clearer error messages
  val newUsernameRegex   = "(?i)[a-z][a-z0-9_-]{0,28}[a-z0-9]".r
  val newUsernamePrefix  = "(?i)^[a-z].*".r
  val newUsernameSuffix  = "(?i).*[a-z0-9]$".r
  val newUsernameChars   = "(?i)^[a-z0-9_-]*$".r
  val newUsernameLetters = "(?i)^([a-z0-9][_-]?)+$".r

  def couldBeUsername(str: UserStr) = noGhost(str.id) && historicalUsernameRegex.matches(str.value)

  def validateId(str: UserStr): Option[UserId] = couldBeUsername(str) option str.id

  def isGhost(id: UserId) = id == ghostId || id.value.startsWith("!")

  def noGhost(id: UserId) = !isGhost(id)

  object BSONFields:
    val id                    = "_id"
    val username              = "username"
    val count                 = "count"
    val enabled               = "enabled"
    val roles                 = "roles"
    val profile               = "profile"
    val toints                = "toints"
    val playTime              = "time"
    val playTimeTotal         = "time.total"
    val createdAt             = "createdAt"
    val seenAt                = "seenAt"
    val kid                   = "kid"
    val createdWithApiVersion = "createdWithApiVersion"
    val lang                  = "lang"
    val title                 = "title"
    val email                 = "email"
    val verbatimEmail         = "verbatimEmail"
    val mustConfirmEmail      = "mustConfirmEmail"
    val prevEmail             = "prevEmail"
    val colorIt               = "colorIt"
    val plan                  = "plan"
    val salt                  = "salt"
    val bpass                 = "bpass"
    val sha512                = "sha512"
    val totpSecret            = "totp"
    val changedCase           = "changedCase"
    val marks                 = "marks"
    val eraseAt               = "eraseAt"
    val erasedAt              = "erasedAt"
    val blind                 = "blind"

  def withFields[A](f: BSONFields.type => A): A = f(BSONFields)

  import lila.db.BSON
  import lila.db.dsl.{ *, given }
  import Plan.given

  given userHandler: BSONDocumentHandler[User] = new BSON[User]:

    import BSONFields.*
    import UserMark.given
    import Count.given
    import Profile.given
    import TotpSecret.given

    def reads(r: BSON.Reader): User =
      val userTitle = r.getO[UserTitle](title)
      User(
        id = r.get[UserId](id),
        username = r.get[UserName](username),
        count = r.get[Count](count),
        enabled = r.get[UserEnabled](enabled),
        roles = ~r.getO[List[String]](roles),
        profile = r.getO[Profile](profile),
        toints = r nIntD toints,
        playTime = r.getO[PlayTime](playTime),
        createdAt = r date createdAt,
        seenAt = r dateO seenAt,
        kid = r boolD kid,
        lang = r strO lang,
        title = userTitle,
        plan = r.getO[Plan](plan) | Plan.empty,
        totpSecret = r.getO[TotpSecret](totpSecret),
        marks = r.getO[UserMarks](marks) | UserMarks.empty
      )

    def writes(w: BSON.Writer, o: User) =
      BSONDocument(
        id         -> o.id,
        username   -> o.username,
        count      -> o.count,
        enabled    -> o.enabled,
        roles      -> o.roles.some.filter(_.nonEmpty),
        profile    -> o.profile,
        toints     -> w.intO(o.toints),
        playTime   -> o.playTime,
        createdAt  -> o.createdAt,
        seenAt     -> o.seenAt,
        kid        -> w.boolO(o.kid),
        lang       -> o.lang,
        title      -> o.title,
        plan       -> o.plan.nonEmpty,
        totpSecret -> o.totpSecret,
        marks      -> o.marks.nonEmpty
      )

  given BSONDocumentHandler[Speaker] = Macros.handler[Speaker]
  given BSONDocumentHandler[Contact] = Macros.handler[Contact]
