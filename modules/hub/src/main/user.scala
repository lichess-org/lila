package lila.hub
package user

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
