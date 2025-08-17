package lila.swiss

import lila.db.dsl.{ *, given }

final private class SwissManualPairing(mongo: SwissMongo)(using Executor):

  def apply(swiss: Swiss): Option[Fu[List[SwissPairing.ByeOrPending]]] =
    swiss.settings.manualPairings.some.filter(_.nonEmpty).map { str =>
      SwissPlayer.fields { p =>
        mongo.player.distinctEasy[UserId, Set](p.userId, $doc(p.swissId -> swiss.id)).map { allUserIds =>
          val parsedLines = str.linesIterator.map {
            _.trim.toLowerCase.split(' ').map(_.trim)
          }.toList
          val pairings = parsedLines.flatMap:
            case Array(u1, u2) if allUserIds(UserId(u1)) && allUserIds(UserId(u2)) && u1 != u2 =>
              SwissPairing.Pending(UserId(u1), UserId(u2)).some
            case _ => none
          val paired = pairings.flatMap { p => List(p.white, p.black) }.toSet
          val byes = parsedLines.flatMap:
            case Array(u1, "1") if !paired(UserId(u1)) => SwissPairing.Bye(UserId(u1)).some
            case _ => none
          pairings.map(Right.apply) ::: byes.map(Left.apply)
        }
      }
    }

private object SwissManualPairing:
  def validate(str: String) =
    str.linesIterator
      .map(_.trim.toLowerCase.split(' ').map(_.trim))
      .foldLeft(Option(Set.empty[UserId])):
        case (Some(prevIds), Array(bye, "1")) if !prevIds(UserId(bye)) =>
          Some(prevIds + UserId(bye))
        case (Some(prevIds), Array(w, b)) if w != b && !prevIds(UserId(w)) && !prevIds(UserId(b)) =>
          Some(prevIds + UserId(w) + UserId(b))
        case _ => None
      .isDefined
