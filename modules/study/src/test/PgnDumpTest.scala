package lila.study

import chess.format.pgn.*
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ Ply, Check, variant }
import Node.*

class PgnDumpTest extends lila.common.LilaTest {

  given PgnDump.WithFlags    = PgnDump.WithFlags(true, true, true, false, false)
  given Conversion[Int, Ply] = Ply(_)

  val P = PgnDump

  def node(ply: Ply, uci: String, san: String, children: Children = emptyChildren) =
    Node(
      id = UciCharPair(Uci(uci).get),
      ply = ply,
      move = Uci.WithSan(Uci(uci).get, SanStr(san)),
      fen = Fen.Epd("<fen>"),
      check = Check.No,
      clock = None,
      crazyData = None,
      children = children,
      forceVariation = false
    )

  def children(nodes: Node*) = Children(nodes.toVector)

  val root = Node.Root.default(variant.Standard)

  test("empty") {
    assertEquals(P.toTurns(root), Vector.empty)
  }

  test("one move") {
    val tree = root.copy(children = children(node(1, "e2e4", "e4")))
    assertMatch(P.toTurns(tree)) {
      case Vector(Turn(1, Some(move), None)) if move.san.value == "e4" && move.variations.isEmpty => true
    }
  }
  test("one move and variation") {
    val tree = root.copy(children =
      children(
        node(1, "e2e4", "e4"),
        node(1, "g1f3", "Nf3")
      )
    )
    assertMatch(P.toTurns(tree)) {
      case Vector(Turn(1, Some(move), None)) if move.san.value == "e4" =>
        move.variations.matchZero:
          case List(List(Turn(1, Some(move), None))) =>
            move.san.value == "Nf3" && move.variations.isEmpty
    }
  }
  test("two moves and one variation") {
    val tree = root.copy(children =
      children(
        node(
          1,
          "e2e4",
          "e4",
          children(
            node(2, "d7d5", "d5")
          )
        ),
        node(1, "g1f3", "Nf3")
      )
    )
    assertMatch(P.toTurns(tree)) {
      case Vector(Turn(1, Some(white), Some(black)))
          if white.san.value == "e4" &&
            black.san.value == "d5" &&
            black.variations.isEmpty =>
        white.variations.matchZero:
          case List(List(Turn(1, Some(move), None))) =>
            move.san.value == "Nf3" && move.variations.isEmpty
    }
  }
  test("two moves and two variations") {
    val tree = root.copy(children =
      children(
        node(
          1,
          "e2e4",
          "e4",
          children(
            node(2, "d7d5", "d5"),
            node(2, "g8f6", "Nf6")
          )
        ),
        node(1, "g1f3", "Nf3")
      )
    )
    assertEquals(P.toTurns(tree).mkString(" ").toString, "1. e4 (1. Nf3) 1... d5 (1... Nf6)")

    assertMatch(P.toTurns(tree)) {
      case Vector(Turn(1, Some(white), Some(black))) if white.san.value == "e4" && black.san.value == "d5" =>
        white.variations.matchZero { case List(List(Turn(1, Some(move), None))) =>
          move.san.value == "Nf3" && move.variations.isEmpty
        } && black.variations.matchZero { case List(List(Turn(1, None, Some(move)))) =>
          move.san.value == "Nf6" && move.variations.isEmpty
        }
    }
  }
  test("more moves and variations") {
    val tree = root.copy(children =
      children(
        node(
          1,
          "e2e4",
          "e4",
          children(
            node(
              2,
              "d7d5",
              "d5",
              children(
                node(3, "a2a3", "a3"),
                node(3, "b2b3", "b3")
              )
            ),
            node(
              2,
              "g8f6",
              "Nf6",
              children(
                node(3, "h2h4", "h4")
              )
            )
          )
        ),
        node(
          1,
          "g1f3",
          "Nf3",
          children(
            node(2, "a7a6", "a6"),
            node(
              2,
              "b7b6",
              "b6",
              children(
                node(3, "c2c4", "c4")
              )
            )
          )
        )
      )
    )
    assertEquals(
      P.toTurns(tree).mkString(" ").toString,
      "1. e4 (1. Nf3 a6 (1... b6 2. c4)) 1... d5 (1... Nf6 2. h4) 2. a3 (2. b3)"
    )

    assertMatch(P.toTurns(tree)) { case Vector(Turn(1, Some(w1), Some(b1)), Turn(2, Some(w2), None)) =>
      w1.san.value == "e4" &&
      w1.variations.matchZero { case List(List(Turn(1, Some(w), Some(b)))) =>
        w.san.value == "Nf3" &&
        w.variations.isEmpty &&
        b.san.value == "a6" &&
        b.variations.matchZero { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
          b.san.value == "b6" &&
          w.san.value == "c4"
        }
      } && b1.san.value == "d5" && b1.variations.matchZero {
        case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
          b.san.value == "Nf6" &&
          b.variations.isEmpty &&
          w.san.value == "h4" &&
          b.variations.isEmpty
      } && w2.san.value == "a3" && w2.variations.matchZero { case List(List(Turn(2, Some(move), None))) =>
        move.san.value == "b3" && move.variations.isEmpty
      }
    }
  }
}
