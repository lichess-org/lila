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
        f.addNewLines(f >> ((f << example).get)) must_== example
      }
    }

    "partial import" in {
      f.addNewLines(f >> ((f << partialBoardFormat).get)) must_== fullBoardFormat
    }

    "hand import" in {
      f.addNewLines(f >> ((f << handInBoard).get)) must_== fullHandInBoard
    }

    "export with special marks" in {
      val board       = Visual << """
k . B . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
N . B . . . . . P
. . . . . . . . .
P P P P P P P P .
. . . . . . . . .
. N S G K G S N L
"""
      val markedBoard = f >>| (board.get, Map(Set(SQ8F, SQ6F, SQ8D, SQ6D, SQ9C, SQ5C, SQ4B, SQ3A) -> 'x'))
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
"""
    }
  }

  val newBoardFormat = """
l n s g k g s n l
. r . . . . . b .
p p p p p p p p p
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
"""

  val partialBoardFormat = """
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
"""

  val fullBoardFormat = """
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
"""

  val handInBoard = """
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
Sente: LN
"""

  val fullHandInBoard = """
Gote:
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . k . . . .
. . . . . . . . .
P P P P P P P P P
. B . . . . . R .
L N S G K G S N L
Sente:NL
"""

  val examples = Seq(
    newBoardFormat,
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
"""
  )
}
