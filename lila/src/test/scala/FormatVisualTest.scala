package lila

import org.specs2.mutable._
import org.specs2.specification._

import format.Visual
import model.Game

class FormatVisualTest extends Specification {

  val f = Visual

  "The visual formatter" should {
    "import and export is non destructive" in {
      pending
      //forall(examples) { example =>
        //(f >> (f << example)) mustEqual example
      //}
    }
  }

  val examples = Seq("""
rnbqkp r
pppppppp



    P n
PPPP   P
RNBQK  R""", """
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
