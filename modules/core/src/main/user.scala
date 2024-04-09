package lila.core
package user

import play.api.i18n.Lang
import chess.PlayerTitle

import lila.core.rating.Perf
import lila.core.perf.PerfKey

opaque type MyId = String
object MyId extends TotalWrapper[MyId, String]:
  given Conversion[MyId, UserId]                 = UserId(_)
  given UserIdOf[MyId]                           = u => u
  given [M[_]]: Conversion[M[MyId], M[UserId]]   = u => UserId.from(MyId.raw(u))
  extension (me: MyId) inline def userId: UserId = me.into(UserId)

case class ChangeEmail(id: UserId, email: EmailAddress)

opaque type UserEnabled = Boolean
object UserEnabled extends YesNo[UserEnabled]

trait User:
  val id: UserId
  val username: UserName
  val title: Option[PlayerTitle]
  val count: Count
  val createdAt: Instant
  val enabled: UserEnabled
  val marks: UserMarks
  val lang: Option[String]
  val roles: List[String]
  val flair: Option[lila.core.Flair]

  def createdSinceDays(days: Int) = createdAt.isBefore(nowInstant.minusDays(days))
  def realLang: Option[Lang]      = lang.flatMap(Lang.get)
  def hasTitle: Boolean           = title.exists(PlayerTitle.BOT != _)
  def isPatron: Boolean
  def light = LightUser(id = id, name = username, title = title, flair = flair, isPatron = isPatron)

object User:
  given UserIdOf[User] = _.id
  given perm.Grantable[User] = new:
    def enabled(u: User) = u.enabled
    def roles(u: User)   = u.roles

case class Me(user: User):
  export user.*

trait WithPerf:
  val user: User
  val perf: Perf
  export user.{ id, createdAt }

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

case class LightPerf(user: LightUser, perfKey: PerfKey, rating: IntRating, progress: IntRatingDiff)
case class LightCount(user: LightUser, count: Int)

trait UserApi:
  def byId[U: UserIdOf](u: U): Fu[Option[User]]
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

abstract class UserRepo(val coll: reactivemongo.api.bson.collection.BSONCollection)
object BSONFields:
  val enabled = "enabled"
  val title   = "title"

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
