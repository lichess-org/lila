package lila.system

import model._
import lila.chess._
import Pos._

class EventStackTest extends SystemTest {

  "an event stack" should {
    "encode and decode" in {
      val stack = EventStack(
        StartEvent(),
        MoveEvent(orig = G4, dest = C3, color = Black),
        PossibleMovesEvent(Map(A7 -> List(A8, B8))),
        PossibleMovesEvent(Map(A2 -> List(A3, A4), F3 -> List(F5, G3, D4, E8))),
        MoveEvent(orig = E5, dest = F6, color = White),
        EnpassantEvent(killed = F5)
        //Event("move", Map("orig" -> E1, "dest" -> C1, "color" -> White)),
        //Event("castling", Map("king" -> List(E1, C1), "rook" -> List(A1, D1), "color" -> White)),
        //Event("redirect", Map("url" -> "http://en.lichess.org/arstheien")),
        //Event("move", Map("orig" -> B7, "dest" -> B8, "color" -> White)),
        //Event("promotion", Map("pieceClass" -> "queen", "key" -> B8)),
        //Event("move", Map("orig" -> B7, "dest" -> B6, "color" -> White)),
        //Event("check", Map("key" -> D6)),
        //Event("message", Map("message" -> List("foo", "http://foto.mail.ru/mail/annabuut/_myphoto/631.html#1491"))),
        //Event("message", Map("message" -> List("0x1", "я слишком красив, чтобы ты это видела=)"))
      )
      EventStack decode stack.encode must_== stack
    }
  }
}
