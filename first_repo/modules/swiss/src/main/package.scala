package lila

import lila.user.User

package object swiss extends PackageObject {

  private[swiss] val logger = lila.log("swiss")

  private[swiss] type Ranking = Map[lila.user.User.ID, Int]

  // FIDE TRF player IDs
  private[swiss] type PlayerIds = Map[User.ID, Int]
  private[swiss] type IdPlayers = Map[Int, User.ID]
}
