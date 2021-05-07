package lila

import lila.user.User

package object swiss extends PackageObject {

  type Ranking = Map[lila.user.User.ID, Int]

  private[swiss] val logger = lila.log("swiss")

  // FIDE TRF player IDs
  private[swiss] type PlayerIds = Map[User.ID, Int]
  private[swiss] type IdPlayers = Map[Int, User.ID]
}
