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

trait User:
  val id: UserId
  val count: Count
  val createdAt: Instant

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
