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
      val tree = root.copy(children = children(node(1, "e3e4", "Pe4")))
      P.toMoves(tree, variant.Standard) must beLike { case Vector(move) =>
        move.usiWithRole.usi.uci must_== "Pe4"
        move.variations must beEmpty
      }
    }
    "one move and variation" in {
      val tree = root.copy(children =
        children(
          node(1, "e3e4", "Pe4"),
          node(1, "c1d2", "Sd2")
        )
      )
      P.toMoves(tree, variant.Standard) must beLike { case Vector(move) =>
        move.usiWithRole.usi.uci must_== "Pe4"
        move.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.uci must_== "Sd2"
          move.variations must beEmpty
        }
      }
    }
    "two moves and one variation" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "e3e4",
            "Pe4",
            children(
              node(2, "d7d6", "Pd6")
            )
          ),
          node(1, "c1d2", "Sd2")
        )
      )
      P.toMoves(tree, variant.Standard) must beLike { case Vector(sente, gote) =>
        sente.usiWithRole.usi.uci must_== "Pe4"
        sente.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.uci must_== "Sd2"
          move.variations must beEmpty
        }
        gote.usiWithRole.usi.uci must_== "Pd6"
        gote.variations must beEmpty
      }
    }
    "two moves and two variations" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "e3e4",
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
        sente.usiWithRole.usi.uci must_== "Pe4"
        sente.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.uci must_== "Sd2"
          move.variations must beEmpty
        }
        gote.usiWithRole.usi.uci must_== "Pd6"
        gote.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.uci must_== "Sd8"
          move.variations must beEmpty
        }
      }
    }
    "more moves and variations" in {
      val tree = root.copy(children =
        children(
          node(
            1,
            "e3e4",
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
        s1.usiWithRole.usi.uci must_== "Pe4"
        s1.variations must beLike { case List(List(w, b)) =>
          w.usiWithRole.usi.uci must_== "Sd2"
          w.variations must beEmpty
          b.usiWithRole.usi.uci must_== "Pa6"
          b.variations must beLike { case List(List(b, w)) =>
            b.usiWithRole.usi.uci must_== "Pb6"
            w.usiWithRole.usi.uci must_== "Pc4"
          }
        }
        g1.usiWithRole.usi.uci must_== "Pd6"
        g1.variations must beLike { case List(List(b, w)) =>
          b.usiWithRole.usi.uci must_== "Sd8"
          b.variations must beEmpty
          w.usiWithRole.usi.uci must_== "Ph4"
          b.variations must beEmpty
        }
        s2.usiWithRole.usi.uci must_== "Pa4"
        s2.variations must beLike { case List(List(move)) =>
          move.usiWithRole.usi.uci must_== "Pb4"
          move.variations must beEmpty
        }
      }
    }
  }
}
