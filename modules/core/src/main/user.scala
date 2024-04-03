package lila.core
package user

import lila.core.rating.Perf

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
  val count: Count
  val createdAt: Instant
  val enabled: UserEnabled
  val marks: UserMarks

  def createdSinceDays(days: Int) = createdAt.isBefore(nowInstant.minusDays(days))

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

enum UserMark:
  case boost
  case engine
  case troll
  case reportban
  case rankban
  case arenaBan
  case prizeBan
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
    def prizeban: Boolean                = hasMark(UserMark.prizeBan)
    def arenaBan: Boolean                = hasMark(UserMark.arenaBan)
    def alt: Boolean                     = hasMark(UserMark.alt)

abstract class UserRepo(val coll: reactivemongo.api.bson.collection.BSONCollection)

trait FlairApi:
  def formField(anyFlair: Boolean, asAdmin: Boolean): play.api.data.Mapping[Option[Flair]]

trait Note:
  val text: String

trait NoteApi:
  def recentByUserForMod(userId: UserId): Fu[Option[Note]]
  def write(to: UserId, text: String, modOnly: Boolean, dox: Boolean)(using MyId): Funit
