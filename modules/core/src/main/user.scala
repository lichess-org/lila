package lila.core

import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler }
import reactivemongo.api.bson.collection.BSONCollection
import play.api.i18n.Lang
import _root_.chess.PlayerTitle

import lila.core.perf.Perf
import lila.core.rating.data.{ IntRating, IntRatingDiff }
import lila.core.perf.{ PerfKey, UserPerfs, UserWithPerfs }
import lila.core.userId.*
import lila.core.email.*
import lila.core.id.Flair
import lila.core.perm.Grantable

object user:

  case class User(
      id: UserId,
      username: UserName,
      count: Count,
      enabled: UserEnabled,
      roles: List[String],
      profile: Option[Profile] = None,
      toints: Int = 0,
      playTime: Option[PlayTime],
      title: Option[PlayerTitle] = None,
      createdAt: Instant,
      seenAt: Option[Instant],
      kid: Boolean,
      lang: Option[String],
      plan: Plan,
      flair: Option[Flair] = None,
      totpSecret: Option[TotpSecret] = None,
      marks: UserMarks = UserMarks(Nil),
      hasEmail: Boolean
  ):
    import lila.core.user.UserMarks.*
    import lila.core.user.UserEnabled.*

    override def equals(other: Any) = other match
      case u: User => id == u.id
      case _       => false

    override def hashCode: Int = id.hashCode

    override def toString =
      s"User $username games:${count.game} ${marks.value.mkString(", ")}"

    def createdSinceDays(days: Int) = createdAt.isBefore(nowInstant.minusDays(days))

    def realLang: Option[Lang] = lang.flatMap(Lang.get)

    def hasTitle: Boolean = title.exists(PlayerTitle.BOT != _)

    def light = LightUser(id = id, name = username, title = title, flair = flair, isPatron = isPatron)

    def profileOrDefault = profile | Profile.default

    def realNameOrUsername = profileOrDefault.nonEmptyRealName | username.value

    def titleUsername: String = title.fold(username.value)(t => s"$t $username")

    def hasGames = count.game > 0

    def countRated = count.rated

    // lazy val seenRecently: Boolean = timeNoSee < User.seenRecently

    def timeNoSee: Duration = (nowMillis - (seenAt | createdAt).toMillis).millis

    def everLoggedIn = seenAt.exists(createdAt != _)

    def lame = marks.boost || marks.engine

    def lameOrTroll      = lame || marks.troll
    def lameOrAlt        = lame || marks.alt
    def lameOrTrollOrAlt = lameOrTroll || marks.alt

    def canBeFeatured = hasTitle && !lameOrTroll

    def canFullyLogin = enabled.yes || !lameOrTrollOrAlt

    def withMarks(f: UserMarks => UserMarks) = copy(marks = f(marks))

    def lightCount = LightCount(light, count.game)

    def isPatron = plan.active

    def activePlan: Option[Plan] = plan.active.option(plan)

    def planMonths: Option[Int] = activePlan.map(_.months)

    def mapPlan(f: Plan => Plan) = copy(plan = f(plan))

    def isBot = title.contains(PlayerTitle.BOT)
    def noBot = !isBot

    def rankable = enabled.yes && noBot && !marks.rankban

    def withPerf(perf: Perf): WithPerf = WithPerf(this, perf)

    def addRole(role: String) = copy(roles = role :: roles)

    import lila.core.perm.Granter
    def isSuperAdmin               = Granter.ofUser(_.SuperAdmin)(this)
    def isAdmin                    = Granter.ofUser(_.Admin)(this)
    def isVerified                 = Granter.ofUser(_.Verified)(this)
    def isApiHog                   = Granter.ofUser(_.ApiHog)(this)
    def isVerifiedOrAdmin          = isVerified || isAdmin
    def isVerifiedOrChallengeAdmin = isVerifiedOrAdmin || Granter.ofUser(_.ApiChallengeAdmin)(this)
  end User

  opaque type KidMode = Boolean
  object KidMode extends YesNo[KidMode]

  opaque type UserEnabled = Boolean
  object UserEnabled extends YesNo[UserEnabled]

  case class PlayTime(total: Int, tv: Int)

  case class Plan(months: Int, active: Boolean, since: Option[Instant])

  case class TotpSecret(secret: Array[Byte]) extends AnyVal:
    override def toString = "TotpSecret(****)"

  case class Profile(
      @Key("country") flag: Option[String] = None,
      location: Option[String] = None,
      bio: Option[String] = None,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      fideRating: Option[Int] = None,
      uscfRating: Option[Int] = None,
      ecfRating: Option[Int] = None,
      rcfRating: Option[Int] = None,
      cfcRating: Option[Int] = None,
      dsbRating: Option[Int] = None,
      links: Option[String] = None
  ):
    def nonEmptyRealName =
      List(ne(firstName), ne(lastName)).flatten match
        case Nil   => none
        case names => (names.mkString(" ")).some

    def nonEmptyLocation = ne(location)

    def nonEmptyBio = ne(bio)

    def isEmpty = completionPercent == 0

    def completionPercent: Int =
      100 * List(flag, bio, firstName, lastName).count(_.isDefined) / 4

    private def ne(str: Option[String]) = str.filter(_.nonEmpty)
  end Profile

  object Profile:
    val default = Profile()

  object User:
    given UserIdOf[User] = _.id
    given perm.Grantable[User] = new:
      def enabled(u: User) = u.enabled
      def roles(u: User)   = u.roles

  case class Count(
      ai: Int,
      draw: Int,
      drawH: Int, // only against human opponents
      game: Int,
      loss: Int,
      lossH: Int, // only against human opponents
      rated: Int,
      win: Int,
      winH: Int // only against human opponents
  )

  case class WithPerf(user: User, perf: Perf):
    export user.{ id, createdAt, hasTitle, light }

  case class LightPerf(user: LightUser, perfKey: PerfKey, rating: IntRating, progress: IntRatingDiff)
  case class LightCount(user: LightUser, count: Int)

  case class ChangeEmail(id: UserId, email: EmailAddress)

  trait UserApi:
    def byId[U: UserIdOf](u: U): Fu[Option[User]]
    def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]]
    def me[U: UserIdOf](u: U): Fu[Option[Me]]
    def email(id: UserId): Fu[Option[EmailAddress]]
    def withEmails[U: UserIdOf](user: U): Fu[Option[WithEmails]]
    def pair(x: UserId, y: UserId): Fu[Option[(User, User)]]
    def emailOrPrevious(id: UserId): Fu[Option[EmailAddress]]
    def enabledByIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]]
    def withIntRatingIn(userId: UserId, perf: PerfKey): Fu[Option[(User, IntRating)]]
    def createdAtById(id: UserId): Fu[Option[Instant]]
    def isEnabled(id: UserId): Fu[Boolean]
    def filterClosedOrInactiveIds(since: Instant)(ids: Iterable[UserId]): Fu[List[UserId]]
    def langOf(id: UserId): Fu[Option[String]]
    def isKid[U: UserIdOf](id: U): Fu[Boolean]
    def isTroll(id: UserId): Fu[Boolean]
    def isBot(id: UserId): Fu[Boolean]
    def filterDisabled(userIds: Iterable[UserId]): Fu[Set[UserId]]
    def isManaged(id: UserId): Fu[Boolean]
    def countEngines(userIds: List[UserId]): Fu[Int]
    def getTitle(id: UserId): Fu[Option[PlayerTitle]]
    def listWithPerfs[U: UserIdOf](us: List[U]): Fu[List[UserWithPerfs]]
    def withPerfs(u: User): Fu[UserWithPerfs]
    def withPerfs[U: UserIdOf](id: U): Fu[Option[UserWithPerfs]]
    def perfsOf[U: UserIdOf](u: U): Fu[UserPerfs]

  trait LightUserApiMinimal:
    val async: LightUser.Getter
    val sync: LightUser.GetterSync
  trait LightUserApi extends LightUserApiMinimal:
    val syncFallback: LightUser.GetterSyncFallback
    def preloadMany(ids: Seq[UserId]): Funit

  case class Emails(current: Option[EmailAddress], previous: Option[NormalizedEmailAddress]):
    def strList = current.map(_.value).toList ::: previous.map(_.value).toList

  case class WithEmails(user: User, emails: Emails)

  enum UserMark:
    case boost
    case engine
    case troll
    case reportban
    case rankban
    case arenaban
    case prizeban
    case alt
    def key = toString

  object UserMark:
    val byKey: Map[String, UserMark] = values.mapBy(_.key)
    val bannable: Set[UserMark]      = Set(boost, engine, troll, alt)

  opaque type UserMarks = List[UserMark]
  object UserMarks extends TotalWrapper[UserMarks, List[UserMark]]:
    extension (a: UserMarks)
      def hasMark(mark: UserMark): Boolean = a.value contains mark
      def dirty                            = a.value.exists(UserMark.bannable.contains)
      def clean                            = !a.dirty
      def boost: Boolean                   = hasMark(UserMark.boost)
      def engine: Boolean                  = hasMark(UserMark.engine)
      def troll: Boolean                   = hasMark(UserMark.troll)
      def reportban: Boolean               = hasMark(UserMark.reportban)
      def rankban: Boolean                 = hasMark(UserMark.rankban)
      def prizeban: Boolean                = hasMark(UserMark.prizeban)
      def arenaBan: Boolean                = hasMark(UserMark.arenaban)
      def alt: Boolean                     = hasMark(UserMark.alt)

  abstract class UserRepo(val coll: BSONCollection):
    given userHandler: BSONDocumentHandler[User]
  abstract class PerfsRepo(val coll: BSONCollection):
    def aggregateLookup: BSONDocument
    def aggregateReadFirst[U: UserIdOf](root: BSONDocument, u: U): UserPerfs

  object BSONFields:
    val enabled = "enabled"
    val title   = "title"
    val roles   = "roles"
    val marks   = "marks"

  trait Note:
    val text: String

  trait NoteApi:
    def recentByUserForMod(userId: UserId): Fu[Option[Note]]
    def write(to: UserId, text: String, modOnly: Boolean, dox: Boolean)(using MyId): Funit

  type FlairMap    = Map[UserId, Flair]
  type FlairGet    = UserId => Fu[Option[Flair]]
  type FlairGetMap = List[UserId] => Fu[FlairMap]
  trait FlairApi:
    given flairOf: FlairGet
    given flairsOf: FlairGetMap
    def formField(anyFlair: Boolean, asAdmin: Boolean): play.api.data.Mapping[Option[Flair]]

  /* User who is currently logged in */
  opaque type Me = User
  object Me extends TotalWrapper[Me, User]:
    export lila.core.userId.MyId as Id
    given UserIdOf[Me]                           = _.id
    given Conversion[Me, User]                   = identity
    given Conversion[Me, UserId]                 = _.id
    given Conversion[Option[Me], Option[UserId]] = _.map(_.id)
    given (using me: Me): LightUser.Me           = me.lightMe
    given [M[_]]: Conversion[M[Me], M[User]]     = Me.raw(_)
    given (using me: Me): Option[Me]             = Some(me)
    given Grantable[Me] = new Grantable[User]:
      def enabled(me: Me) = me.enabled
      def roles(me: Me)   = me.roles
    extension (me: Me)
      def userId: UserId        = me.id
      def lightMe: LightUser.Me = LightUser.Me(me.value.light)
      inline def modId: ModId   = userId.into(ModId)
      inline def myId: MyId     = userId.into(MyId)
    // given (using me: Me): LightUser.Me = LightUser.Me(me.light)

  final class Flag(val code: Flag.Code, val name: Flag.Name, val abrev: Option[String]):
    def shortName = abrev | name

  object Flag:
    type Code = String
    type Name = String

  trait FlagApi:
    val all: List[Flag]
    val nonCountries: List[Flag.Code]
