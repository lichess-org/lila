package lila.study

import chess.format.pgn._
import chess.format.{ FEN, Uci, UciCharPair }
import chess.variant
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
      crazyData = None,
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
      val tree = root.copy(children = children(node(1, "e2e4", "e4")))
      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(move), None)) =>
        move.san must_== "e4"
        move.variations must beEmpty
      }
    }
    "one move and variation" in {
      val tree = root.copy(children =
        children(
          node(1, "e2e4", "e4"),
          node(1, "g1f3", "Nf3")
        )
      )
      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(move), None)) =>
        move.san must_== "e4"
        move.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san must_== "Nf3"
          move.variations must beEmpty
        }
      }
    }
    "two moves and one variation" in {
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
      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(white), Some(black))) =>
        white.san must_== "e4"
        white.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san must_== "Nf3"
          move.variations must beEmpty
        }
        black.san must_== "d5"
        black.variations must beEmpty
      }
    }
    "two moves and two variations" in {
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
      P.toTurns(tree).mkString(" ").toString must_==
        "1. e4 (1. Nf3) 1... d5 (1... Nf6)"

      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(white), Some(black))) =>
        white.san must_== "e4"
        white.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san must_== "Nf3"
          move.variations must beEmpty
        }
        black.san must_== "d5"
        black.variations must beLike { case List(List(Turn(1, None, Some(move)))) =>
          move.san must_== "Nf6"
          move.variations must beEmpty
        }
      }
    }
    "more moves and variations" in {
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
      P.toTurns(tree).mkString(" ").toString must_==
        "1. e4 (1. Nf3 a6 (1... b6 2. c4)) 1... d5 (1... Nf6 2. h4) 2. a3 (2. b3)"

      P.toTurns(tree) must beLike { case Vector(Turn(1, Some(w1), Some(b1)), Turn(2, Some(w2), None)) =>
        w1.san must_== "e4"
        w1.variations must beLike { case List(List(Turn(1, Some(w), Some(b)))) =>
          w.san must_== "Nf3"
          w.variations must beEmpty
          b.san must_== "a6"
          b.variations must beLike { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
            b.san must_== "b6"
            w.san must_== "c4"
          }
        }
        b1.san must_== "d5"
        b1.variations must beLike { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
          b.san must_== "Nf6"
          b.variations must beEmpty
          w.san must_== "h4"
          b.variations must beEmpty
        }
        w2.san must_== "a3"
        w2.variations must beLike { case List(List(Turn(2, Some(move), None))) =>
          move.san must_== "b3"
          move.variations must beEmpty
        }
      }
    }
  }
}
