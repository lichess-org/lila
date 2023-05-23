package lila.study

import chess.format.pgn.*
import chess.format.pgn.PgnTree.*
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ Ply, Check, variant }
import Node.*

import lila.tree.{ Branch, Branches, Root }

class PgnDumpTest extends lila.common.LilaTest {

  given Conversion[Int, Ply] = Ply(_)

  val P = PgnDump

  def node(ply: Ply, uci: String, san: String, children: Branches = Branches.empty) =
    Branch(
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

  def children(nodes: Branch*) = Branches(nodes.toList)

  val root = Root.default(variant.Standard)

  import Helpers.rootToPgn

  test("empty") {
    assertEquals(rootToPgn(root).value, "")
  }

  test("one move") {
    val tree = root.copy(children = children(node(0, "e2e4", "e4")))
    assertEquals(rootToPgn(tree).value, "1. e4")
  }

  test("one move and variation") {
    val tree = root.copy(children =
      children(
        node(0, "e2e4", "e4"),
        node(0, "g1f3", "Nf3")
      )
    )
    assertEquals(rootToPgn(tree).value, "1. e4 (1. Nf3)")
  }

  test("two moves and one variation") {
    val tree = root.copy(children =
      children(
        node(
          0,
          "e2e4",
          "e4",
          children(
            node(1, "d7d5", "d5")
          )
        ),
        node(0, "g1f3", "Nf3")
      )
    )
    assertEquals(rootToPgn(tree).value, "1. e4 (1. Nf3) 1... d5")
  }

  test("two moves and two variations") {
    val tree = root.copy(children =
      children(
        node(
          0,
          "e2e4",
          "e4",
          children(
            node(1, "d7d5", "d5"),
            node(1, "g8f6", "Nf6")
          )
        ),
        node(0, "g1f3", "Nf3")
      )
    )
    assertEquals(rootToPgn(tree).value, "1. e4 (1. Nf3) 1... d5 (1... Nf6)")
  }

  test("more moves and variations") {
    val tree = root.copy(children =
      children(
        node(
          0,
          "e2e4",
          "e4",
          children(
            node(
              1,
              "d7d5",
              "d5",
              children(
                node(2, "a2a3", "a3"),
                node(2, "b2b3", "b3")
              )
            ),
            node(
              1,
              "g8f6",
              "Nf6",
              children(
                node(2, "h2h4", "h4")
              )
            )
          )
        ),
        node(
          0,
          "g1f3",
          "Nf3",
          children(
            node(1, "a7a6", "a6"),
            node(
              1,
              "b7b6",
              "b6",
              children(
                node(2, "c2c4", "c4")
              )
            )
          )
        )
      )
    )
    assertEquals(
      rootToPgn(tree).value,
      "1. e4 (1. Nf3 a6 (1... b6 2. c4)) 1... d5 (1... Nf6 2. h4) 2. a3 (2. b3)"
    )
  }

}
