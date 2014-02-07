package lila.puzzle

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(
  puzzleColl: Coll,
  attemptColl: Coll) {

  def apply(me: Option[User]): Fu[Puzzle] = ???
  // me match {
  //   case None => puzzleColl

}
