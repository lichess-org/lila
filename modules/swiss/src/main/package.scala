package lila

package object swiss extends PackageObject {

  private[swiss] val logger = lila.log("swiss")

  private[swiss] type Ranking     = Map[lila.user.User.ID, Int]
  private[swiss] type RankingSwap = Map[Int, lila.user.User.ID]
}
