package lila.system

import model._
import lila.chess._
import Pos._

class EventStackTest extends SystemTest {

  "an event stack" should {
    "encode and decode all events without loss" in {
      val stack = EventStack(
        StartEvent(),
        MoveEvent(orig = G4, dest = C3, color = Black),
        PossibleMovesEvent(Map(A7 -> List(A8, B8))),
        PossibleMovesEvent(Map(A2 -> List(A3, A4), F3 -> List(F5, G3, D4, E8))),
        MoveEvent(orig = E5, dest = F6, color = White),
        EnpassantEvent(killed = F5),
        MoveEvent(orig = E1, dest = C1, color = White),
        CastlingEvent(king = (E1, C1), rook = (A1, D1), color = White),
        RedirectEvent(url = "http://en.lichess.org/arstheien"),
        MoveEvent(orig = B7, dest = B8, color = White),
        PromotionEvent(role = Queen, pos = B8),
        MoveEvent(orig = A8, dest = D8, color = White),
        CheckEvent(pos = D6),
        MessageEvent(author = "foo", message = "http://foto.mail.ru/mail/annabuut/_myphoto/631.html#1491"),
        MessageEvent(author = "thibault", message = "message with a | inside"),
        MessageEvent(author = "0x1", message = "я слишком красив, чтобы ты это видела=)"),
        ThreefoldEvent(),
        ReloadTableEvent(),
        MoretimeEvent(White, 15),
        EndEvent()
      )
      EventStack decode stack.encode must_== stack
    }
    "decode and re-encode production data events" in {
      dbGame5.players.forall { player =>
        (EventStack decode player.evts).encode must_== player.evts
      }
    }
    "optimize events" in {
      "empty duplicated possible move events" in {
        EventStack(
          StartEvent(),
          MoveEvent(orig = G4, dest = C3, color = Black),
          PossibleMovesEvent(Map(A7 -> List(A8, B8))),
          MoveEvent(orig = E5, dest = F6, color = White),
          PossibleMovesEvent(Map(A2 -> List(A3, A4), F3 -> List(F5, G3, D4, E8))),
          MoveEvent(orig = G4, dest = C3, color = Black),
          PossibleMovesEvent(Map(A5 -> List(A8, B8))),
          MoretimeEvent(White, 15),
          EndEvent()
        ).optimize must_== EventStack(
          StartEvent(),
          MoveEvent(orig = G4, dest = C3, color = Black),
          PossibleMovesEvent(Map()),
          MoveEvent(orig = E5, dest = F6, color = White),
          PossibleMovesEvent(Map()),
          MoveEvent(orig = G4, dest = C3, color = Black),
          PossibleMovesEvent(Map(A5 -> List(A8, B8))),
          MoretimeEvent(White, 15),
          EndEvent()
        )
      }
      "keep only the %d more recent events" format EventStack.maxEvents in {
        val nb = EventStack.maxEvents
        val someEvent = CheckEvent(pos = D6)
        val endEvent = EndEvent()
        val events = List.fill(nb + 40)(someEvent) :+ endEvent
        val stack = EventStack(events: _*)
        val expected = (List.fill(nb - 1)(someEvent) :+ endEvent)
        stack.optimize.events.toList map (_._2) must_== expected
      }
    }
  }
}
