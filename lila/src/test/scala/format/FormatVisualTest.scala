package lila
package format

import org.specs2.mutable._
import org.specs2.specification._

import model.Board

class VisualTest extends Specification {

  val f = Visual

  "The visual board formatter" should {
    "export new board" in {
      newLines(f >> (Board())) mustEqual newBoardFormat
    }
    "import and export is non destructive" in {
      forall(examples) { example =>
        newLines(f >> (f << example)) mustEqual example
      }
    }
  }

  def newLines(str: String) = "\n" + str + "\n"

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
