package lila.tournament

import ornicar.scalalib.Zero
import lila.user.User

/* https://en.wikipedia.org/wiki/Double_hashing
 * immutable interface for open addressing hash tables,
 * mapping from users to zero-index ranks
 * performance gain over Map[String, Int] due:
 * a) class doesn't need to support modification operations after building
 * b) ranks always in a range from 0 to users.length - 1, no need to boxing Ints
 * c) due special building order, it is possible answer to queries 'is user has lower rank than given'
 *    with less number of comparisions
 */
private[tournament] class RankingMap(users: Array[User.ID]) {
  private[this] val bits = {
    val l = users.length
    val k = 32 - Integer.numberOfLeadingZeros(l)
    if (1.5 * l < (1 << k)) k else k + 1
  }
  private[this] val mask = (1 << bits) - 1
  private[this] val h    = Array.fill(1 << bits)(Int.MaxValue)
  private[this] def seek(key: User.ID, rank: Int): Int = {
    val hc  = key.hashCode
    var pos = hc & mask
    //use odd steps, since step should be relative prime to hash size equal to 2^bits
    val step = ((hc >>> (bits - 1)) & bits) | 1
    while (h(pos) < rank && users(h(pos)) != key) {
      pos = (pos + step) & mask
    }
    pos
  }
  //adding better ranked users first (c)
  for (i <- 0 until users.length) {
    val pos = seek(users(i), i)
    h(pos) = i
  }
  /* betterRank stops searching after finding slot with rank not lesser than rank parameter
   * returns Some(rank)
   */
  final def betterRank(key: User.ID, rank: Int): Option[Int] = {
    val k = h(seek(key, rank))
    k < Int.MaxValue option k
  }
  final def get(key: User.ID): Option[Int] = betterRank(key, Int.MaxValue)
  final def size                           = users.length
}

object RankingMap {
  final val empty = new RankingMap(Array.empty[User.ID])
  object implicits {
    implicit final val rankingMapZero: Zero[RankingMap] = Zero.instance(empty)
  }
}
