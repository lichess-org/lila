package lila.swiss

import lila.db.dsl.{ *, given }
import lila.swiss.BsonHandlers.given
import lila.user.User

final private class SwissManualPairing(mongo: SwissMongo)(using scala.concurrent.ExecutionContext):

  def apply(swiss: Swiss): Option[Fu[List[SwissPairing.ByeOrPending]]] =
    swiss.settings.manualPairings.some.filter(_.nonEmpty) map { str =>
      SwissPlayer.fields { p =>
        val selectPresentPlayers = $doc(p.swissId -> swiss.id, p.absent $ne true)
        mongo.player.distinctEasy[UserId, Set](p.userId, selectPresentPlayers) map { presentUserIds =>
          val pairings = str.linesIterator.flatMap {
            _.trim.toLowerCase.split(' ').map(_.trim) match
              case Array(u1, u2) if presentUserIds(UserId(u1)) && presentUserIds(UserId(u2)) && u1 != u2 =>
                SwissPairing.Pending(UserId(u1), UserId(u2)).some
              case _ => none
          }.toList
          val paired   = pairings.flatMap { p => List(p.white, p.black) }.toSet
          val unpaired = presentUserIds diff paired
          val byes     = unpaired map { u => SwissPairing.Bye(u) }
          pairings.map(Right.apply) ::: byes.toList.map(Left.apply)
        }
      }
    }

private object SwissManualPairing:
  def validate(str: String) =
    str.linesIterator
      .map(_.trim.toLowerCase.split(' ').map(_.trim))
      .foldLeft(Option(Set.empty[UserId])) {
        case (Some(prevIds), Array(w, b)) if w != b && !prevIds(UserId(w)) && !prevIds(UserId(b)) =>
          Some(prevIds + UserId(w) + UserId(b))
        case _ => None
      }
      .isDefined
