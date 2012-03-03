package lila.system

import model._

class EventStackTest extends SystemTest {

  "an event stack" should {
    "encode and decode" in {
      val stack = EventStack(List(
        Event("start"),
        Event("move", Map("from" -> "g4", "to" -> "c3", "color" -> "black")),
        Event("possible_moves", Map("possible_moves" -> Map("a7" -> "a8b8"))),
        Event("possible_moves", Map("possible_moves" -> Map("a2" -> "a3a4", "f3" -> "f5g3d4e8"))),
        Event("move", Map("from" -> "e5", "to" -> "f6", "color" -> "white")),
        Event("enpassant", Map("killed" -> "f5")),
        Event("move", Map("from" -> "e1", "to" -> "c1", "color" -> "white")),
        Event("castling", Map("king" -> List("e1", "c1"), "rook" -> List("a1", "d1"), "color" -> "white")),
        Event("redirect", Map("url" -> "http://en.lichess.org/arstheien")),
        Event("move", Map("from" -> "b7", "to" -> "b8", "color" -> "white")),
        Event("promotion", Map("pieceClass" -> "queen", "key" -> "b8")),
        Event("move", Map("from" -> "b7", "to" -> "b6", "color" -> "white")),
        Event("check", Map("key" -> "d6")),
        Event("message", Map("message" -> List("foo", "http://foto.mail.ru/mail/annabuut/_myphoto/631.html#1491"))),
        Event("message", Map("message" -> List("0x1", "я слишком красив, чтобы ты это видела=)")
        ))
      ))
      EventStack decode stack.encode must_== stack
    }
  }
}
