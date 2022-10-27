package lila.swiss

import lila.db.dsl._
import lila.user.User

final private class SwissManualPairing(colls: SwissColls)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BsonHandlers._

  def apply(swiss: Swiss): Option[Fu[List[SwissPairing.ByeOrPending]]] =
    swiss.settings.manualPairings.some.filter(_.nonEmpty) map { str =>
      SwissPlayer.fields { p =>
        colls.player.distinctEasy[User.ID, Set](p.userId, $doc(p.swissId -> swiss.id)) map { userIds =>
          val pairings = str.linesIterator.flatMap {
            _.trim.toLowerCase.split(' ').map(_.trim) match {
              case Array(u1, u2) if userIds(u1) && userIds(u2) && u1 != u2 =>
                SwissPairing.Pending(u1, u2).some
              case _ => none
            }
          }.toList
          val paired = userIds diff pairings.flatMap { p => List(p.white, p.black) }.toSet
          val byes   = userIds map { u => SwissPairing.Bye(u) }
          pairings.map(Right.apply) ::: byes.toList.map(Left.apply)
        }
      }
    }
}
