package lila.study

import shogi.format.forsyth.Sfen
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.variant
import Node._
import org.specs2.mutable._

class NotationDumpTest extends Specification {

  implicit private val flags = NotationDump.WithFlags(
    csa = false,
    comments = true,
    variations = true,
    shiftJis = false,
    clocks = true
  )

  val P = NotationDump

  def node(ply: Int, usi: String, children: Children = emptyChildren) =
    Node(
      id = UsiCharPair(Usi(usi).get, variant.Standard),
      ply = ply,
      usi = Usi(usi).get,
      sfen = Sfen("<sfen>"),
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
      val tree = root.copy(children = children(node(1, "5g5f")))
      P.toMoves(tree, variant.Standard) must beLike { case Vector(move) =>
        move.usiWithRole.usi.usi must_== "5g5f"
        move.variations must beEmpty
      }
    }
    "one move and variation" in {
      val tree = root.copy(children =
        children(
          node(1, "5g5f"),
          node(1, "7i6h")
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
            children(
              node(2, "3c3d")
            )
          ),
          node(1, "7i6h")
        )
      )
      P.toMoves(tree, variant.Standard) must beLike { case Vector(sente, gote) =>
        sente.usiWithRole.usi.usi must_== "5g5f"
        sente.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "7i6h"
          move.variations must beEmpty
        }
        gote.usiWithRole.usi.usi must_== "3c3d"
        gote.variations must beEmpty
      }
    }
    "two moves and two variations" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "5g5f",
            children(
              node(2, "3c3d"),
              node(2, "5c5d")
            )
          ),
          node(1, "7i6h")
        )
      )

      P.toMoves(tree, variant.Standard) must beLike { case Vector(sente, gote) =>
        sente.usiWithRole.usi.usi must_== "5g5f"
        sente.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "7i6h"
          move.variations must beEmpty
        }
        gote.usiWithRole.usi.usi must_== "3c3d"
        gote.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.usi must_== "5c5d"
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
            children(
              node(
                2,
                "3c3d",
                children(
                  node(3, "9g9f"),
                  node(3, "8g8f")
                )
              ),
              node(
                2,
                "5c5d",
                children(
                  node(3, "2h5h")
                )
              )
            )
          ),
          node(
            1,
            "7i6h",
            children(
              node(2, "9c9d"),
              node(
                2,
                "8c8d",
                children(
                  node(3, "7g7f")
                )
              )
            )
          )
        )
      )

      P.toMoves(tree, variant.Standard) must beLike { case Vector(s1, g1, s2) =>
        s1.usiWithRole.usi.usi must_== "5g5f"
        s1.variations must beLike { case List(List(s, g)) =>
          s.usiWithRole.usi.usi must_== "7i6h"
          s.variations must beEmpty
          g.usiWithRole.usi.usi must_== "9c9d"
          g.variations must beLike { case List(List(g, s)) =>
            g.usiWithRole.usi.usi must_== "8c8d"
            s.usiWithRole.usi.usi must_== "7g7f"
          }
        }
        g1.usiWithRole.usi.usi must_== "3c3d"
        g1.variations must beLike { case List(List(g, s)) =>
          g.usiWithRole.usi.usi must_== "5c5d"
          g.variations must beEmpty
          s.usiWithRole.usi.usi must_== "2h5h"
          g.variations must beEmpty
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
