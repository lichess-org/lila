package lila.chess

import Pos._

class KnightTest extends LilaTest {

  "a knight" should {

    val knight = White - Knight

    def moves(pos: Pos): Valid[List[Pos]] = Board.empty place knight at pos flatMap { b ⇒
      b actorAt pos map (_.destinations)
    }

    "move in any of 8 positions, 2 and 1 squares away" in {
      moves(E4) must bePoss(F6, G5, G3, F2, D2, C3, C5, D6)
    }

    "move 2 and 1 squares away, even when at the edges" in {
      moves(H8) must bePoss(G6, F7)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
k B

   B
    P
  N
    P
PPP  PPP
 NBQKBNR
"""
      board destsFrom C4 must bePoss(board, """
k B

 x B
x   P
  N
x   P
PPPx PPP
 NBQKBNR
""")
    }

    "capture opponent pieces" in {
      val board = """
k B

 b B
n
  N
    b
PPP  PPP
 NBQKBNR
"""
      board destsFrom C4 must bePoss(board, """
k B

 x B
x   x
  N
x   x
PPPx PPP
 NBQKBNR
""")
    }
    "threaten" in {
      val board = """
k B

 b B
n
  N
    Q
PPP  PPP
 NBQKBNR
"""
      "a reachable enemy" in {
        board actorAt C4 map (_ threatens A5) must succeedWith(true)
      }
      "an unreachable enemy" in {
        board actorAt C4 map (_ threatens A8) must succeedWith(false)
      }
      "a reachable friend" in {
        board actorAt C4 map (_ threatens E3) must succeedWith(false)
      }
      "nothing left" in {
        board actorAt C4 map (_ threatens B4) must succeedWith(false)
      }
      "nothing up" in {
        board actorAt C4 map (_ threatens C5) must succeedWith(false)
      }
    }
  }
}
