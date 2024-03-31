package lila.core
package user

opaque type MyId = String
object MyId extends TotalWrapper[MyId, String]:
  given Conversion[MyId, UserId]                 = UserId(_)
  given UserIdOf[MyId]                           = u => u
  given [M[_]]: Conversion[M[MyId], M[UserId]]   = u => UserId.from(MyId.raw(u))
  extension (me: MyId) inline def userId: UserId = me.into(UserId)

trait User:
  def count: Count
  def createdAt: Instant

  def createdSinceDays(days: Int) = createdAt.isBefore(nowInstant.minusDays(days))

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
