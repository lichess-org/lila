package lila.swiss

import lila.rating.Perf
import lila.user.{ Perfs, User }

case class SwissPlayer(
    _id: SwissPlayer.Id, // random
    swissId: Swiss.Id,
    number: SwissPlayer.Number,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    points: Swiss.Points,
    score: Swiss.Score
) {
  def id                              = _id
  def is(uid: User.ID): Boolean       = uid == userId
  def is(user: User): Boolean         = is(user.id)
  def is(other: SwissPlayer): Boolean = is(other.userId)
}

object SwissPlayer {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)

  private[swiss] def make(
      swissId: Swiss.Id,
      number: SwissPlayer.Number,
      user: User,
      perfLens: Perfs => Perf
  ): SwissPlayer = new SwissPlayer(
    _id = makeId,
    swissId = swissId,
    number = number,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional,
    points = Swiss.Points(0),
    score = Swiss.Score(0)
  )

  case class Number(value: Int) extends AnyVal with IntValue

  case class Ranked(rank: Int, player: SwissPlayer) {
    def is(other: Ranked) = player is other.player
    override def toString = s"$rank. ${player.userId}[${player.rating}]"
  }

  // def ranked(ranking: Ranking)(player: SwissPlayer): Option[Ranked] =
  //   ranking get player.userId map { rank =>
  //     Ranked(rank + 1, player)
  //   }
}
