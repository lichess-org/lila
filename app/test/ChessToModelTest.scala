package lila

import model._
import chess._

import format.Visual
import Pos._

class ChessToModelTest extends SystemTest {

  "chess to model conversion" should {
    "new game" in {
      val dbGame = newDbGame
      val game = dbGame.toChess
      "identity" in {
        val dbg2 = dbGame.update(game, anyMove)
        "white pieces" in {
          sortPs((dbg2 player White).ps) must_== sortPs((dbGame player White).ps)
        }
        "black pieces" in {
          sortPs((dbg2 player Black).ps) must_== sortPs((dbGame player Black).ps)
        }
      }
    }
    "played game" in {
      val dbGame = dbGame4
      val game = Game(
        board = """
r   kb r
ppp pppp
  np
    P
   P   N
  P B P
P P  P P
R  QK  q
""",
        player = White,
        pgnMoves = dbGame.pgn,
        deads = List(
          C3 -> Black.knight,
          F5 -> Black.bishop,
          F5 -> White.bishop,
          C3 -> White.knight,
          H1 -> White.rook
        ))
      "identity" in {
        val dbg2 = dbGame.update(game, anyMove)
        "white pieces" in {
          sortPs((dbg2 player White).ps) must_== sortPs((dbGame player White).ps)
        }
        "black pieces" in {
          sortPs((dbg2 player Black).ps) must_== sortPs((dbGame player Black).ps)
        }
      }
      "new pieces positions" in {
        val dbg2 = newDbGame.update(game, anyMove)
        "white pieces" in {
          sortPs((dbg2 player White).ps) must_== sortPs((dbGame player White).ps)
        }
        "black pieces" in {
          sortPs((dbg2 player Black).ps) must_== sortPs((dbGame player Black).ps)
        }
      }
    }
    "update events" in {
      def playerEvents(dbg: Valid[DbGame], color: Color) =
        dbg.toOption map (g ⇒ (g player color).eventStack.events)
      "simple move" in {
        val dbg = newDbGame.withoutEvents.afterMove(D2, D4)
        "white events" in {
          playerEvents(dbg, White) must_== Some(Seq(
            1 -> MoveEvent(D2, D4, White),
            2 -> PossibleMovesEvent(Map.empty)
          ))
        }
        "black events" in {
          playerEvents(dbg, Black) must_== Some(Seq(
            1 -> MoveEvent(D2, D4, White),
            2 -> PossibleMovesEvent((Map(G7 -> List(G6, G5), F7 -> List(F6, F5), D7 -> List(D6, D5), A7 -> List(A6, A5), G8 -> List(F6, H6), C7 -> List(C6, C5), B8 -> List(A6, C6), B7 -> List(B6, B5), H7 -> List(H6, H5), E7 -> List(E6, E5))))
          ))
        }
      }
      "check" in {
        val dbg = newDbGameWithBoard("""
   r

PPPP   P
RNBQK  R
""").copy(turns = 11).withoutEvents.afterMove(D4, E4)
        "white events" in {
          playerEvents(dbg, White) must beSome.like {
            case events ⇒ events map (_._2) must contain(CheckEvent(E1))
          }
        }
        "black events" in {
          playerEvents(dbg, Black) must beSome.like {
            case events ⇒ events map (_._2) must contain(CheckEvent(E1))
          }
        }
      }
      "check mate" in {
        val dbg = newDbGameWithBoard("""
   r

PPPP P P
RNBRKR R
""").copy(turns = 11).withoutEvents.afterMove(D4, E4)
        "white events" in {
          playerEvents(dbg, White) must beSome.like {
            case events ⇒ events map (_._2) must contain(CheckEvent(E1))
          }
        }
        "black events" in {
          playerEvents(dbg, Black) must beSome.like {
            case events ⇒ events map (_._2) must contain(CheckEvent(E1))
          }
        }
      }
      "stale mate" in {
        val dbg = newDbGameWithBoard("""
p
 rr
K
""").copy(turns = 11).withoutEvents.afterMove(A3, A2)
        "white events" in {
          playerEvents(dbg, White) must beSome.like {
            case events ⇒ events map (_._2) must contain(
              PossibleMovesEvent(Map.empty))
          }
        }
        "black events" in {
          playerEvents(dbg, Black) must beSome.like {
            case events ⇒ events map (_._2) must contain(
              PossibleMovesEvent(Map.empty))
          }
        }
      }
      "en passant" in {
        val dbg = newDbGameWithBoard("""
  Pp


PP P P P
RNBRKR R
""").copy(turns = 10, lastMove = "d7 d5".some).withoutEvents.afterMove(C5, D6)
        "white events" in {
          playerEvents(dbg, White) must beSome.like {
            case events ⇒ events map (_._2) must contain(EnpassantEvent(D5))
          }
        }
        "black events" in {
          playerEvents(dbg, Black) must beSome.like {
            case events ⇒ events map (_._2) must contain(EnpassantEvent(D5))
          }
        }
      }
    }
  }

  def sortPs(ps: String): String = ps.split(' ').toList.sorted mkString " "
}
