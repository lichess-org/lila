package lila.study

import chess.format.pgn.*
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ Ply, Check, variant }
import org.specs2.mutable._

import lila.tree.{ Branch, Branches, Root }

class PgnDumpTest extends Specification {

  given PgnDump.WithFlags    = PgnDump.WithFlags(true, true, true, false, false)
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

  "toTurns" >> {
    "empty" >> {
      P.toTurns(root) must beEmpty
    }
    "one move" >> {
      val tree = root.copy(children = children(node(1, "e2e4", "e4")))
      P.toTurns(tree) must beLike { case List(Turn(1, Some(move), None)) =>
        move.san === "e4"
        move.variations must beEmpty
      }
    }
    "one move and variation" >> {
      val tree = root.copy(children =
        children(
          node(1, "e2e4", "e4"),
          node(1, "g1f3", "Nf3")
        )
      )
      P.toTurns(tree) must beLike { case List(Turn(1, Some(move), None)) =>
        move.san === "e4"
        move.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san === "Nf3"
          move.variations must beEmpty
        }
      }
    }
    "two moves and one variation" >> {
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
      P.toTurns(tree) must beLike { case List(Turn(1, Some(white), Some(black))) =>
        white.san === "e4"
        white.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san === "Nf3"
          move.variations must beEmpty
        }
        black.san === "d5"
        black.variations must beEmpty
      }
    }
    "two moves and two variations" >> {
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
      P.toTurns(tree).mkString(" ").toString ===
        "1. e4 (1. Nf3) 1... d5 (1... Nf6)"

      P.toTurns(tree) must beLike { case List(Turn(1, Some(white), Some(black))) =>
        white.san === "e4"
        white.variations must beLike { case List(List(Turn(1, Some(move), None))) =>
          move.san === "Nf3"
          move.variations must beEmpty
        }
        black.san === "d5"
        black.variations must beLike { case List(List(Turn(1, None, Some(move)))) =>
          move.san === "Nf6"
          move.variations must beEmpty
        }
      }
    }
    "more moves and variations" >> {
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
      P.toTurns(tree).mkString(" ").toString ===
        "1. e4 (1. Nf3 a6 (1... b6 2. c4)) 1... d5 (1... Nf6 2. h4) 2. a3 (2. b3)"

      P.toTurns(tree) must beLike { case List(Turn(1, Some(w1), Some(b1)), Turn(2, Some(w2), None)) =>
        w1.san === "e4"
        w1.variations must beLike { case List(List(Turn(1, Some(w), Some(b)))) =>
          w.san === "Nf3"
          w.variations must beEmpty
          b.san === "a6"
          b.variations must beLike { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
            b.san === "b6"
            w.san === "c4"
          }
        }
        b1.san === "d5"
        b1.variations must beLike { case List(List(Turn(1, None, Some(b)), Turn(2, Some(w), None))) =>
          b.san === "Nf6"
          b.variations must beEmpty
          w.san === "h4"
          b.variations must beEmpty
        }
        w2.san === "a3"
        w2.variations must beLike { case List(List(Turn(2, Some(move), None))) =>
          move.san === "b3"
          move.variations must beEmpty
        }
      }
    }
  }
}
