package lila.chess
package format

import Pos._

class VisualTest extends ChessTest {

  val f = Visual

  "The visual board formatter" should {
    "export new board" in {
      f.addNewLines(f >> (Board())) must_== newBoardFormat
    }
    "import new board" in {
      f << newBoardFormat must_== Board()
    }
    "import and export is non destructive" in {
      forall(examples) { example ⇒
        f.addNewLines(f >> (f << example)) must_== example
      }
    }
    "import partial board representation" in {
      f << """
    P n
PPPP   P
RNBQK  R""" must_== (f << """





    P n
PPPP   P
RNBQK  R
""")
    }
    "export with special marks" in {
      val board = Visual << """
k B



N B    P

PPPPPPPP
 NBQKBNR
"""
      val markedBoard = f >>|(board, Map(Set(B3, D3, B5, D5, A6, E6, F7, G8) -> 'x'))
      f addNewLines markedBoard must_== """
k B   x
     x
x   x
 x x
N B    P
 x x
PPPPPPPP
 NBQKBNR
"""
    }
  }

  val newBoardFormat = """
rnbqkbnr
pppppppp




PPPPPPPP
RNBQKBNR
"""

  val examples = Seq(newBoardFormat,
    """
rnbqkp r
pppppppp



    P n
PPPP   P
RNBQK  R
""", """
       k
       P





K
""", """
rnbqkbnr
pppppppp





RK    NR
""", """
  bqkb r
p ppp pp
pr
   P p
   QnB
 PP  N
P    PPP
RN  K  R
""", """
r   k nr
pp n ppp
  p p
q
 b P B
P N  Q P
 PP BPP
R   K  R
"""
  )
}
