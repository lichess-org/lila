package shogi
package format

import Pos._

class VisualTest extends ShogiTest {

  val f = Visual

  "The visual board formatter" should {
    "export new board" in {
      f.addNewLines(f >> makeBoard) must_== newBoardFormat
    }

    "import and export is non destructive" in {
      forall(examples) { example =>
        f.addNewLines(f >> (f << example)) must_== example
      }
    }

    "export with special marks" in {
      val board       = Visual << """
k B



N B     P

PPPPPPPPP

 NSGKGSNL
"""
      val markedBoard = f >>| (board, Map(Set(B4, D4, B6, D6, A7, E7, F8, G9) -> 'x'))
      f addNewLines markedBoard must_== """
k B   x
     x
x   x
 x x
N B     P
 x x
PPPPPPPPP

 NSGKGSNL
"""
    }
  }

  val newBoardFormat = """
lnsgkgsnl
 r     b
ppppppppp



PPPPPPPPP
 B     R
LNSGKGSNL
"""

  val examples = Seq(
    newBoardFormat,
    """
lnsgkgsnl
 r     B
pppppp pp
      p

  P
PP PPPPPP
       R
LNSGKGSNL
""",
    """
       k
       P






K
""",
    """
lnsgkgsnl
 r     b
ppppppppp





LK     NL
""",
    """
  bgkg nl
p ppp ppp
 r
   P p

    nB
 PP  N  P
P    PPP
LNS K   L
"""
  )
}
