package lila.study

import chess.format.pgn.*
import chess.format.{ Fen, Uci }
import chess.{ Ply, variant }

import lila.tree.{ Branch, Branches, Clock, Node as TreeNode, Root }

class PgnDumpTest extends munit.FunSuite:

  given Conversion[Int, Ply] = Ply(_)

  val P = PgnDump

  def node(ply: Ply, uci: String, san: String, children: Branches = Branches.empty) =
    Branch(
      ply = ply,
      move = Uci.WithSan(Uci(uci).get, SanStr(san)),
      fen = Fen.Full("<fen>"),
      clock = None,
      crazyData = None,
      children = children,
      forceVariation = false
    )

  def children(nodes: Branch*) = Branches(nodes.toList)

  val root = Root.default(variant.Standard)

  import Helpers.rootToPgn

  test("empty"):
    assertEquals(rootToPgn(root).value, "")

  test("one move"):
    val tree = root.copy(children = children(node(1, "e2e4", "e4")))
    assertEquals(rootToPgn(tree).value, "1. e4")

  test("one move and variation"):
    val tree = root.copy(children =
      children(
        node(1, "e2e4", "e4"),
        node(1, "g1f3", "Nf3")
      )
    )
    assertEquals(rootToPgn(tree).value, "1. e4 (1. Nf3)")

  test("two moves and one variation"):
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
    assertEquals(rootToPgn(tree).value, "1. e4 (1. Nf3) 1... d5")

  test("two moves and two variations"):
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
    assertEquals(rootToPgn(tree).value, "1. e4 (1. Nf3) 1... d5 (1... Nf6)")

  test("more moves and variations"):
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
      rootToPgn(tree).value,
      "1. e4 (1. Nf3 a6 (1... b6 2. c4)) 1... d5 (1... Nf6 2. h4) 2. a3 (2. b3)"
    )

  test("disabled flags"):
    val move = node(1, "e2e4", "e4").copy(
      comments = TreeNode.Comments(
        List(TreeNode.Comment(TreeNode.Comment.Id("i"), Comment("note"), TreeNode.Comment.Author.Lichess))
      ),
      clock = Clock(chess.Centis(6000)).some
    )
    val tree = root.copy(children = children(move, node(1, "g1f3", "Nf3")))
    val flags = P.fullFlags.copy(comments = false, variations = false, clocks = false)
    assertEquals(P.rootToPgn(tree, Tags.empty)(using flags).render.value, "1. e4")

  test("force variation"):
    val tree = root.copy(children = children(node(1, "e2e4", "e4").copy(forceVariation = true)))
    assertEquals(rootToPgn(tree).value, "1. e4 (1. e4)")

  test("max supported depth"):
    val mainline = Node.MAX_PLIES
      .to(1)
      .by(-1)
      .foldLeft(none[Branch]): (child, ply) =>
        val (uci, san) = if ply % 2 == 1 then "e2e4" -> "e4" else "e7e5" -> "e5"
        node(ply, uci, san, child.fold(Branches.empty)(children(_))).some
    val tree = root.copy(children = children(mainline.get))
    assert(rootToPgn(tree).value.endsWith("300. e4 e5"))
