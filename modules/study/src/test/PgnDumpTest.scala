package lila.study

import shogi.Data
import shogi.format.pgn._
import shogi.format.{ FEN, Uci, UciCharPair }
import shogi.variant
import Node._
import org.specs2.mutable._

class PgnDumpTest extends Specification {

  implicit private val flags = PgnDump.WithFlags(true, true, true)

  val P = PgnDump

  def node(ply: Int, uci: String, san: String, children: Children = emptyChildren) =
    Node(
      id = UciCharPair(Uci(uci).get),
      ply = ply,
      move = Uci.WithSan(Uci(uci).get, san),
      fen = FEN("<fen>"),
      check = false,
      clock = None,
      crazyData = Some(Data.init),
      children = children,
      forceVariation = false
    )

  def children(nodes: Node*) = Children(nodes.toVector)

  val root = Node.Root.default(variant.Standard)

  "toTurns" should {
    "empty" in {
      P.toTurns(root) must beEmpty
    }
    "one move" in {
      val tree = root.copy(children = children(node(1, "e3e4", "Pe4")))
      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(move), None)) =>
        move.san must_== "Pe4"
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
      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(move), None)) =>
        move.san must_== "Pe4"
        move.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san must_== "Sd2"
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
      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(sente), Some(gote))) =>
        sente.san must_== "Pe4"
        sente.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san must_== "Sd2"
          move.variations must beEmpty
        }
        gote.san must_== "Pd6"
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
      P.toTurns(tree).mkString(" ").toString must_==
        "1. Pe4 (1. Sd2) 1... Pd6 (1... Sd8)"

      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(sente), Some(gote))) =>
        sente.san must_== "Pe4"
        sente.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san must_== "Sd2"
          move.variations must beEmpty
        }
        gote.san must_== "Pd6"
        gote.variations must beLike { case List(List(Turn(1, None, Some(move)))) =>
          move.san must_== "Sd8"
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
      P.toTurns(tree).mkString(" ").toString must_==
        "1. Pe4 (1. Sd2 Pa6 (1... Pb6 2. Pc4)) 1... Pd6 (1... Sd8 2. Ph4) 2. Pa4 (2. Pb4)"

      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(s1), Some(g1)), Turn(2, Some(s2), None)) =>
        s1.san must_== "Pe4"
        s1.variations must beLike { case List(List(Turn(1, Some(w), Some(b)))) =>
          w.san must_== "Sd2"
          w.variations must beEmpty
          b.san must_== "Pa6"
          b.variations must beLike { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
            b.san must_== "Pb6"
            w.san must_== "Pc4"
          }
        }
        g1.san must_== "Pd6"
        g1.variations must beLike { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
          b.san must_== "Sd8"
          b.variations must beEmpty
          w.san must_== "Ph4"
          b.variations must beEmpty
        }
        s2.san must_== "Pa4"
        s2.variations must beLike { case List(List(Turn(2, Some(move), None))) =>
          move.san must_== "Pb4"
          move.variations must beEmpty
        }
      }
    }
  }
}
