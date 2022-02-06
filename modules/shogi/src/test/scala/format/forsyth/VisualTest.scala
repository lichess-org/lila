package shogi
package format
package forsyth

import Pos._

class VisualTest extends ShogiTest {

  val f = Visual

  "The visual board formatter" should {
    "export new board" in {
      f.addNewLines(f render makeSituation) must_== newVisualFormat
    }

    "import and export is non destructive" in {
      forall(examples) { example =>
        f.addNewLines(f render ((f parse example).get)) must_== example
      }
    }

    "partial import" in {
      f.addNewLines(f render ((f parse partialSituationFormat).get)) must_== fullSituationFormat
    }

    "hand import" in {
      f.addNewLines(f render ((f parse handInBoard).get)) must_== fullHandInBoard
    }

    "export with special marks" in {
      val situation = Visual parse """
k . B . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
N . B . . . . . P
. . . . . . . . .
P P P P P P P P .
. . . . . . . . .
. N S G K G S N L
Hands:
Turn:Sente
"""
      val markedBoard =
        f render (situation.get, Map(Set(SQ8F, SQ6F, SQ8D, SQ6D, SQ9C, SQ5C, SQ4B, SQ3A) -> 'x'))
      f addNewLines markedBoard must_== """
k . B . . . x . .
. . . . . x . . .
x . . . x . . . .
. x . x . . . . .
N . B . . . . . P
. x . x . . . . .
P P P P P P P P .
. . . . . . . . .
. N S G K G S N L
Hands:
Turn:Sente
"""
    }
  }

  val newVisualFormat = """
l n s g k g s n l
. r . . . . . b .
p p p p p p p p p
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
Hands:
Turn:Sente
"""

  val partialSituationFormat = """
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
"""

  val fullSituationFormat = """
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
Hands:
Turn:Sente
"""

  val handInBoard = """
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
Hands:2lB
Turn:Sente
"""

  val fullHandInBoard = """
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
Hands:B2l
Turn:Sente
"""

  val examples = Seq(
    newVisualFormat,
    """
l n s g k g s n l
. r . . . . . B .
p p p p p p . p p
. . . . . . p . .
. . . . . . . . .
. . P . . . . . .
P P . P P P P P P
. . . . . . . R .
L N S G K G S N L
Hands:
Turn:Sente
""",
    """
. . . . . . k . .
. . . . . . P . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
K . . . . . . . .
Hands:
Turn:Gote
""",
    """
l n s g k g s n l
. r . . . . . b .
p p p p p p p p p
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
L K . . . . . N L
Hands:
Turn:Sente
""",
    """
. . b g k g . n l
p . p p p . p p p
. r . . . . . . .
. . P . p . . . .
. . . . . . . . .
. . . . n B . . .
. P P . . N . . P
P . . . . P P P .
L N S . K . . . L
Hands:B2r10p
Turn:Sente
"""
  )
}
