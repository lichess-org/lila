package lila.core

import _root_.chess.{ Color, ByColor, PlayerTitle, IntRating }
import _root_.chess.rating.IntRatingDiff
import _root_.chess.rating.glicko.Glicko
import play.api.i18n.Lang
import play.api.libs.json.JsObject
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler, BSONDocumentReader }
import scalalib.model.{ Days, LangTag }

import lila.core.email.*
import lila.core.id.Flair
import lila.core.perf.{ KeyedPerf, Perf, PerfKey, UserPerfs, UserWithPerfs }
import lila.core.userId.*
import lila.core.misc.AtInstant

object user:

  case class User(
      id: UserId,
      username: UserName,
      count: Count,
      enabled: UserEnabled,
      roles: List[RoleDbKey],
      profile: Option[Profile] = None,
      toints: Int = 0,
      playTime: Option[PlayTime],
      title: Option[PlayerTitle] = None,
      createdAt: Instant,
      seenAt: Option[Instant],
      kid: KidMode,
      lang: Option[LangTag],
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
      case _ => false

    override def hashCode: Int = id.hashCode

    override def toString =
      s"User $username games:${count.game} ${marks.value.mkString(", ")}"

    def createdSinceDays(days: Int) = createdAt.isBefore(nowInstant.minusDays(days))

    def realLang: Option[Lang] = lang.flatMap: tag =>
      Lang.get(tag.value)

    def hasTitle: Boolean = title.exists(PlayerTitle.BOT != _)

    def light = LightUser(id, username, title, flair, isPatron = isPatron)

    def profileOrDefault = profile | Profile.default

    def realNameOrUsername = profileOrDefault.nonEmptyRealName | username.value

    def titleUsername: String = title.fold(username.value)(t => s"$t $username")

    def everLoggedIn = seenAt.exists(createdAt != _)

    def lame = marks.boost || marks.engine
    def lameOrTroll = lame || marks.troll

    def withMarks(f: UserMarks => UserMarks) = copy(marks = f(marks))

    def isPatron = plan.active

    def isBot = title.contains(PlayerTitle.BOT)
    def noBot = !isBot

    def rankable = enabled.yes && noBot && !marks.rankban

    def withPerf(perf: Perf): WithPerf = WithPerf(this, perf)

    def addRole(role: RoleDbKey) = copy(roles = role :: roles)

    import lila.core.perm.Granter
    def isSuperAdmin = Granter.ofUser(_.SuperAdmin)(this)
    def isAdmin = Granter.ofUser(_.Admin)(this)
    def isVerified = Granter.ofUser(_.Verified)(this)
    def isApiHog = Granter.ofUser(_.ApiHog)(this)
    def isVerifiedOrAdmin = isVerified || isAdmin
    def isVerifiedOrChallengeAdmin = isVerifiedOrAdmin || Granter.ofUser(_.ApiChallengeAdmin)(this)
  end User

  opaque type RoleDbKey = String
  object RoleDbKey extends OpaqueString[RoleDbKey]

  opaque type KidMode = Boolean
  object KidMode extends YesNo[KidMode]

  opaque type UserEnabled = Boolean
  object UserEnabled extends YesNo[UserEnabled]

  case class PlayTime(total: Int, tv: Int)

  case class Plan(months: Int, active: Boolean, since: Option[Instant]):
    def isEmpty: Boolean = months == 0
    def nonEmpty: Option[Plan] = Option.when(!isEmpty)(this)

  case class TotpSecret(secret: Array[Byte]) extends AnyVal:
    override def toString = "TotpSecret(****)"

  case class Profile(
      @Key("country") flag: Option[FlagCode] = None,
      location: Option[String] = None,
      bio: Option[String] = None,
      realName: Option[String] = None,
      fideRating: Option[Int] = None,
      uscfRating: Option[Int] = None,
      ecfRating: Option[Int] = None,
      rcfRating: Option[Int] = None,
      cfcRating: Option[Int] = None,
      dsbRating: Option[Int] = None,
      links: Option[String] = None
  ):
    def nonEmptyRealName = ne(realName)

    def nonEmptyLocation = ne(location)

    def nonEmptyBio = ne(bio)

    def isEmpty = completionPercent == 0

    def completionPercent: Int =
      100 * List(flag, bio, realName).count(_.isDefined) / 3

    private def ne(str: Option[String]) = str.filter(_.nonEmpty)

  end Profile

  object Profile:
    val default = Profile()

  object User:
    given UserIdOf[User] = _.id
    given AtInstant[User] = _.createdAt

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

  case class ChangeEmail(id: UserId, email: EmailAddress)

  case class UserDelete(user: User):
    export user.id

  trait UserApi:
    def byId[U: UserIdOf](u: U): Fu[Option[User]]
    def enabledById[U: UserIdOf](u: U): Fu[Option[User]]
    def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]]
    def byIdAs[U: BSONDocumentReader](id: String, proj: BSONDocument): Fu[Option[U]]
    def me[U: UserIdOf](u: U): Fu[Option[Me]]
    def email(id: UserId): Fu[Option[EmailAddress]]
    def withEmails[U: UserIdOf](user: U): Fu[Option[WithEmails]]
    def pair(x: UserId, y: UserId): Fu[Option[(User, User)]]
    def emailOrPrevious(id: UserId): Fu[Option[EmailAddress]]
    def enabledByIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]]
    def withIntRatingIn(userId: UserId, perf: PerfKey): Fu[Option[(User, IntRating)]]
    def createdAtById(id: UserId): Fu[Option[Instant]]
    def isEnabled(id: UserId): Fu[Boolean]
    def langOf(id: UserId): Fu[Option[String]]
    def isKid[U: UserIdOf](id: U): Fu[KidMode]
    def isTroll(id: UserId): Fu[Boolean]
    def isBot(id: UserId): Fu[Boolean]
    def filterExists(ids: Set[UserId]): Fu[List[UserId]]
    def filterClosedOrInactiveIds(since: Instant)(ids: Iterable[UserId]): Fu[List[UserId]]
    def filterDisabled(userIds: Iterable[UserId]): Fu[Set[UserId]]
    def filterLame(ids: Seq[UserId]): Fu[Set[UserId]]
    def filterKid[U: UserIdOf](ids: Seq[U]): Fu[Set[UserId]]
    def isManaged(id: UserId): Fu[Boolean]
    def countEngines(userIds: List[UserId]): Fu[Int]
    def filterEngines(userIds: Seq[UserId]): Fu[Set[UserId]]
    def getTitle(id: UserId): Fu[Option[PlayerTitle]]
    def withPerf(id: User, pk: PerfKey): Fu[WithPerf]
    def withPerfs(u: User): Fu[UserWithPerfs]
    def withPerfs[U: UserIdOf](id: U): Fu[Option[UserWithPerfs]]
    def byIdWithPerf[U: UserIdOf](id: U, pk: PerfKey): Fu[Option[WithPerf]]
    def listWithPerfs[U: UserIdOf](us: List[U]): Fu[List[UserWithPerfs]]
    def perfOf[U: UserIdOf](u: U, perfKey: PerfKey): Fu[Perf]
    def perfOf(ids: Iterable[UserId], perfKey: PerfKey): Fu[Map[UserId, Perf]]
    def perfOptionOf[U: UserIdOf](u: U, perfKey: PerfKey): Fu[Option[Perf]]
    def perfsOf[U: UserIdOf](u: U): Fu[UserPerfs]
    def perfsOf[U: UserIdOf](us: PairOf[U], primary: Boolean): Fu[PairOf[UserPerfs]]
    def dubiousPuzzle(id: UserId, puzzle: Perf): Fu[Boolean]
    def setPerf(userId: UserId, pk: PerfKey, perf: Perf): Funit
    def userIdsWithRoles(roles: List[RoleDbKey]): Fu[Set[UserId]]
    def incColor(userId: UserId, color: Color): Unit
    def firstGetsWhite(u1: UserId, u2: UserId): Fu[Boolean]
    def firstGetsWhite(u1O: Option[UserId], u2O: Option[UserId]): Fu[Boolean]
    def mustPlayAsColor(userId: UserId): Fu[Option[Color]]
    def gamePlayersAny(userIds: ByColor[Option[UserId]], perf: PerfKey): Fu[GameUsers]
    def gamePlayersLoggedIn(
        ids: ByColor[UserId],
        perf: PerfKey,
        useCache: Boolean = true
    ): Fu[Option[ByColor[WithPerf]]]
    def glicko(userId: UserId, perf: PerfKey): Fu[Option[Glicko]]
    def containsDisabled(userIds: Iterable[UserId]): Fu[Boolean]
    def containsEngine(userIds: List[UserId]): Fu[Boolean]
    def usingPerfOf[A, U: UserIdOf](u: U, perfKey: PerfKey)(f: Perf ?=> Fu[A]): Fu[A]
    def incToints(id: UserId, nb: Int): Funit
    def addPuzRun(field: String, userId: UserId, score: Int): Funit
    def setPlan(user: User, plan: Option[Plan]): Funit
    def filterByEnabledPatrons(userIds: List[UserId]): Fu[Set[UserId]]
    def isCreatedSince(id: UserId, since: Instant): Fu[Boolean]
    def accountAge(id: UserId): Fu[Days]
    def visibleBotsByIds(ids: Iterable[UserId]): Fu[List[UserWithPerfs]]

  trait LightUserApiMinimal:
    val async: LightUser.Getter
    val sync: LightUser.GetterSync
  trait LightUserApi extends LightUserApiMinimal:
    val syncFallback: LightUser.GetterSyncFallback
    val asyncFallback: LightUser.GetterFallback
    def asyncMany(ids: List[UserId]): Fu[List[Option[LightUser]]]
    def asyncManyFallback(ids: Seq[UserId]): Fu[Seq[LightUser]]
    def preloadMany(ids: Seq[UserId]): Funit
    def preloadUser(user: User): Unit
    def invalidate(id: UserId): Unit
    val isBotSync: LightUser.IsBotSync

  case class Emails(current: Option[EmailAddress], previous: Option[NormalizedEmailAddress]):
    def strList = current.map(_.value).toList ::: previous.map(_.value).toList

  case class WithEmails(user: User, emails: Emails)

  enum UserMark:
    case boost
    case engine
    case troll
    case isolate
    case reportban
    case rankban
    case arenaban
    case prizeban
    case alt
    def key = toString

  object UserMark:
    val byKey: Map[String, UserMark] = values.mapBy(_.key)
    val bannable: Set[UserMark] = Set(boost, engine, troll, alt)

  opaque type UserMarks = List[UserMark]
  object UserMarks extends TotalWrapper[UserMarks, List[UserMark]]:
    extension (a: UserMarks)
      def hasMark(mark: UserMark): Boolean = a.value contains mark
      def dirty = a.value.exists(UserMark.bannable.contains)
      def clean = !a.dirty
      def boost: Boolean = hasMark(UserMark.boost)
      def engine: Boolean = hasMark(UserMark.engine)
      def troll: Boolean = hasMark(UserMark.troll)
      def isolate: Boolean = hasMark(UserMark.isolate)
      def reportban: Boolean = hasMark(UserMark.reportban)
      def rankban: Boolean = hasMark(UserMark.rankban)
      def prizeban: Boolean = hasMark(UserMark.prizeban)
      def arenaBan: Boolean = hasMark(UserMark.arenaban)
      def alt: Boolean = hasMark(UserMark.alt)

  abstract class UserRepo(val coll: BSONCollection):
    given userHandler: BSONDocumentHandler[User]
    given planHandler: BSONDocumentHandler[Plan]
  abstract class PerfsRepo(val coll: BSONCollection):
    def aggregateLookup: BSONDocument
    def aggregateReadFirst[U: UserIdOf](root: BSONDocument, u: U): UserPerfs

  object BSONFields:
    val enabled = "enabled"
    val title = "title"
    val roles = "roles"
    val marks = "marks"
    val username = "username"
    val flair = "flair"
    val plan = "plan"
    val kid = "kid"
    val createdAt = "createdAt"

  trait Note:
    val text: String

  trait NoteApi:
    def recentToUserForMod(userId: UserId): Fu[Option[Note]]
    def write(to: UserId, text: String, modOnly: Boolean, dox: Boolean)(using MyId): Funit
    def lichessWrite(to: User, text: String): Funit

  abstract class RankingRepo(val coll: lila.core.db.AsyncCollFailingSilently)

  type FlairMap = Map[UserId, Flair]
  type FlairGet = UserId => Fu[Option[Flair]]
  type FlairGetMap = List[UserId] => Fu[FlairMap]
  trait FlairApi:
    given flairOf: FlairGet
    given flairsOf: FlairGetMap
    val adminFlairs: Set[Flair]
    def formField(anyFlair: Boolean = false, asAdmin: Boolean = false): play.api.data.Mapping[Option[Flair]]
    def find(name: String): Option[Flair]

  /* User who is currently logged in */
  opaque type Me = User
  object Me extends TotalWrapper[Me, User]:
    export lila.core.userId.MyId as Id
    given UserIdOf[Me] = _.id
    given Conversion[Me, User] = identity
    given Conversion[Me, UserId] = _.id
    given Conversion[Option[Me], Option[UserId]] = _.map(_.id)
    given (using me: Me): LightUser.Me = me.lightMe
    given [M[_]]: Conversion[M[Me], M[User]] = Me.raw(_)
    given (using me: Me): Option[Me] = Some(me)
    extension (me: Me)
      def userId: UserId = me.id
      def lightMe: LightUser.Me = LightUser.Me(me.value.light)
      inline def modId: ModId = userId.into(ModId)
      inline def myId: MyId = userId.into(MyId)

  final class Flag(val code: FlagCode, val name: FlagName, val abrev: Option[String]):
    def shortName = abrev | name

  opaque type FlagCode = String
  object FlagCode extends OpaqueString[FlagCode]
  type FlagName = String

  trait FlagApi:
    val all: List[Flag]
    val nonCountries: List[FlagCode]
    def name(flag: Flag): FlagName

  type GameUser = Option[WithPerf]
  type GameUsers = ByColor[GameUser]

  type PublicFideIdOf = LightUser => Fu[Option[_root_.chess.FideId]]

  object TrophyKind:
    val marathonWinner = "marathonWinner"
    val marathonTopTen = "marathonTopTen"
    val marathonTopFifty = "marathonTopFifty"
    val marathonTopHundred = "marathonTopHundred"
    val marathonTopFivehundred = "marathonTopFivehundred"

  trait TrophyApi:
    def award(trophyUrl: String, userId: UserId, kindKey: String): Funit

  trait CachedApi:
    def getTop50Online: Fu[List[UserWithPerfs]]
    def getBotIds: Fu[Set[UserId]]
    def userIdsLike(text: UserSearch): Fu[List[UserId]]

  trait JsonView:
    def full(u: User, perfs: Option[UserPerfs | KeyedPerf], withProfile: Boolean): JsObject
