package lila.system

import model._
import lila.chess._
import Pos._
import org.specs2.matcher.{ Expectable, Matcher }

class EventStackTest extends SystemTest {

  "encode and decode all events without loss" in {
    val stack = EventStack.build(
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
    dbGame5.players.forall { player ⇒
      (EventStack decode player.evts).encode must_== player.evts
    }
  }
  "optimize events" in {
    "empty duplicated possible move events" in {
      EventStack.build(
        StartEvent(),
        MoveEvent(orig = G4, dest = C3, color = Black),
        PossibleMovesEvent(Map(A7 -> List(A8, B8))),
        MoveEvent(orig = E5, dest = F6, color = White),
        PossibleMovesEvent(Map(A2 -> List(A3, A4), F3 -> List(F5, G3, D4, E8))),
        MoveEvent(orig = G4, dest = C3, color = Black),
        PossibleMovesEvent(Map(A5 -> List(A8, B8))),
        MoretimeEvent(White, 15),
        EndEvent()
      ).optimize must_== EventStack.build(
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
      val stack = EventStack.build(events: _*)
      val expected = (List.fill(nb - 1)(someEvent) :+ endEvent)
      stack.optimize.events.toList map (_._2) must_== expected
    }
  }
  "apply move events" in {
    def addMoves(eventStack: EventStack, moves: Move*) = moves.foldLeft(eventStack) {
      case (stack, move) ⇒ stack withEvents (Event fromMove move)
    }
    "start with no events" in {
      EventStack().events must beEmpty
    }
    "move" in {
      addMoves(EventStack(), newMove(
        piece = White.pawn, orig = D2, dest = D4
      )).events must_== Seq(
        1 -> MoveEvent(D2, D4, White)
      )
    }
    "capture" in {
      addMoves(EventStack(), newMove(
        piece = White.pawn, orig = D2, dest = E3, capture = Some(E3)
      )).events must_== Seq(
        1 -> MoveEvent(D2, E3, White)
      )
    }
    "enpassant" in {
      addMoves(EventStack(), newMove(
        piece = White.pawn, orig = D5, dest = E6, capture = Some(E5), enpassant = true
      )).events must_== Seq(
        1 -> MoveEvent(D5, E6, White),
        2 -> EnpassantEvent(E5)
      )
    }
    "promotion" in {
      addMoves(EventStack(), newMove(
        piece = White.pawn, orig = D7, dest = D8, promotion = Some(Rook)
      )).events must_== Seq(
        1 -> MoveEvent(D7, D8, White),
        2 -> PromotionEvent(Rook, D8)
      )
    }
    "castling" in {
      addMoves(EventStack(), newMove(
        piece = White.king, orig = E1, dest = G1, castle = (H1, F1).some
      )).events must_== Seq(
        1 -> MoveEvent(E1, G1, White),
        2 -> CastlingEvent((E1, G1), (H1, F1), White)
      )
    }
    "two moves" in {
      addMoves(EventStack(),
        newMove(piece = White.pawn, orig = D2, dest = D4),
        newMove(piece = Black.pawn, orig = D7, dest = D5)
      ).events must_== Seq(
          1 -> MoveEvent(D2, D4, White),
          2 -> MoveEvent(D7, D5, Black)
        )
    }
  }
  "get versions" in {
    val stack = EventStack(List(
        21 -> ThreefoldEvent(),
        23 -> EndEvent(),
        22 -> ThreefoldEvent(),
        19 -> StartEvent()
    ))
    "first version" in {
      stack.firstVersion must_== 19
    }
    "last version" in {
      stack.lastVersion must_== 23
    }
  }
  "get events" in {
    "since version" in {
      "empty events" in {
        EventStack().eventsSince(12) must beNone
      }
      val threeEvents = List(
        20 -> StartEvent(),
        21 -> ThreefoldEvent(),
        22 -> EndEvent()
      )
      val threeEventsValues = threeEvents map (_._2)
      val unordered = List(
        21 -> ThreefoldEvent(),
        22 -> EndEvent(),
        20 -> StartEvent()
      )
      "too old version" in {
        EventStack(threeEvents).eventsSince(12) must beNone
      }
      "too new version" in {
        EventStack(threeEvents).eventsSince(23) must beNone
      }
      "latest version" in {
        EventStack(threeEvents).eventsSince(22) must_== Some(Nil)
      }
      "first version" in {
        EventStack(threeEvents).eventsSince(19) must_== Some(threeEventsValues)
      }
      "first version, unordered events" in {
        EventStack(unordered).eventsSince(19) must_== Some(threeEventsValues)
      }
      "latest version, unordered events" in {
        EventStack(unordered).eventsSince(19) must_== Some(threeEventsValues)
      }
      "middle version, unordered events" in {
        EventStack(unordered).eventsSince(21) must_== Some(List(
          EndEvent()
        ))
      }
    }
  }
}
