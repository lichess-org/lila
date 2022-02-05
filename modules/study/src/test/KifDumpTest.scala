package lila.study

import shogi.format.FEN
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.variant
import Node._
import org.specs2.mutable._

class KifDumpTest extends Specification {

  implicit private val flags = NotationDump.WithFlags(false, true, true, true)

  val P = NotationDump

  def node(ply: Int, usi: String, san: String, children: Children = emptyChildren) =
    Node(
      id = UsiCharPair(Usi.Move(usi).get),
      ply = ply,
      usi = Usi.Move(usi).get,
      fen = FEN("<sfen>"),
      check = false,
      clock = None,
      children = children,
      forceVariation = false
    )

  def children(nodes: Node*) = Children(nodes.toVector)

  val root = Node.Root.default(variant.Standard)

  "toMoves" should {
    "empty" in {
      P.toMoves(root, variant.Standard) must beEmpty
    }
    "one move" in {
      val tree = root.copy(children = children(node(1, "5g5f", "Pe4")))
      P.toMoves(tree, variant.Standard) must beLike { case Vector(move) =>
        move.usiWithRole.usi.usi must_== "5g5f"
        move.variations must beEmpty
      }
    }
    "one move and variation" in {
      val tree = root.copy(children =
        children(
          node(1, "5g5f", "Pe4"),
          node(1, "c1d2", "Sd2")
        )
      )
      P.toMoves(tree, variant.Standard) must beLike { case Vector(move) =>
        move.usiWithRole.usi.usi must_== "5g5f"
        move.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "7i6h"
          move.variations must beEmpty
        }
      }
    }
    "two moves and one variation" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "5g5f",
            "Pe4",
            children(
              node(2, "d7d6", "Pd6")
            )
          ),
          node(1, "c1d2", "Sd2")
        )
      )
      P.toMoves(tree, variant.Standard) must beLike { case Vector(sente, gote) =>
        sente.usiWithRole.usi.usi must_== "5g5f"
        sente.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "7i6h"
          move.variations must beEmpty
        }
        gote.usiWithRole.usi.usi must_== "6c6d"
        gote.variations must beEmpty
      }
    }
    "two moves and two variations" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "5g5f",
            "Pe4",
            children(
              node(2, "d7d6", "Pd6"),
              node(2, "c9d8", "Sd8")
            )
          ),
          node(1, "c1d2", "Sd2")
        )
      )

      P.toMoves(tree, variant.Standard) must beLike { case Vector(sente, gote) =>
        sente.usiWithRole.usi.usi must_== "5g5f"
        sente.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "7i6h"
          move.variations must beEmpty
        }
        gote.usiWithRole.usi.usi must_== "6c6d"
        gote.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "7a6b"
          move.variations must beEmpty
        }
      }
    }
    "more moves and variations" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "5g5f",
            "Pe4",
            children(
              node(
                2,
                "d7d6",
                "Pd6",
                children(
                  node(3, "a3a4", "Pa4"),
                  node(3, "b3b4", "Pb4")
                )
              ),
              node(
                2,
                "c9d8",
                "Sd8",
                children(
                  node(3, "h3h4", "Ph4")
                )
              )
            )
          ),
          node(
            1,
            "c1d2",
            "Sd2",
            children(
              node(2, "a7a6", "Pa6"),
              node(
                2,
                "b7b6",
                "Pb6",
                children(
                  node(3, "c3c4", "Pc4")
                )
              )
            )
          )
        )
      )

      P.toMoves(tree, variant.Standard) must beLike { case Vector(s1, g1, s2) =>
        s1.usiWithRole.usi.usi must_== "5g5f"
        s1.variations must beLike { case List(List(w, b)) =>
          w.usiWithRole.usi.usi must_== "7i6h"
          w.variations must beEmpty
          b.usiWithRole.usi.usi must_== "9c9d"
          b.variations must beLike { case List(List(b, w)) =>
            b.usiWithRole.usi.usi must_== "8c8d"
            w.usiWithRole.usi.usi must_== "7g7f"
          }
        }
        g1.usiWithRole.usi.usi must_== "6c6d"
        g1.variations must beLike { case List(List(b, w)) =>
          b.usiWithRole.usi.usi must_== "7a6b"
          b.variations must beEmpty
          w.usiWithRole.usi.usi must_== "2g2f"
          b.variations must beEmpty
        }
        s2.usiWithRole.usi.usi must_== "9g9f"
        s2.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "8g8f"
          move.variations must beEmpty
        }
      }
    }
  }
}
