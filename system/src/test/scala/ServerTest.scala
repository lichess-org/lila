package lila.system

import model._

class ServerTest extends SystemTest {

  val env = SystemEnv()
  val repo = env.gameRepo
  val server = env.server

  def insert() = {
    val game = newDbGameWithRandomIds
    repo insert game
    game
  }
  def move(game: DbGame, m: String = "d2 d4") = for {
    player ← game playerByColor "white"
    fullId ← game fullIdOf player
  } yield server.playMove(fullId, m)

  "the server" should {
    "play a single move" in {
      "report success" in {
        val game = insert()
        move(game) must beSome.like { case r ⇒ r must beSuccess }
      }
      "be persisted" in {
        "update turns" in {
          val game = insert()
          move(game)
          repo game game.id must beSome.like {
            case g ⇒ g.turns must_== 1
          }
        }
        "update board" in {
          val game = insert()
          move(game)
          repo game game.id must beSome.like {
            case g ⇒ addNewLines(g.toChess.board.visual) must_== """
rnbqkbnr
pppppppp


   P

PPP PPPP
RNBQKBNR
"""
          }
        }
      }
    }
    "play the Peruvian Immortal" in {

      val moves = List("e2 e4", "d7 d5", "e4 d5", "d8 d5", "b1 c3", "d5 a5", "d2 d4", "c7 c6", "g1 f3", "c8 g4", "c1 f4", "e7 e6", "h2 h3", "g4 f3", "d1 f3", "f8 b4", "f1 e2", "b8 d7", "a2 a3", "e8 c8", "a3 b4", "a5 a1", "e1 d2", "a1 h1", "f3 c6", "b7 c6", "e2 a6")

      def play(game: DbGame) = for(m <- moves) yield move(game, m).get

      "report success" in {
        val game = insert()
        sequenceValid(play(game)) must beSuccess
      }
      "be persisted" in {
        "update turns" in {
          val game = insert()
          play(game)
          repo game game.id must beSome.like {
            case g ⇒ g.turns must_== 27
          }
        }
        "update board" in {
          val game = insert()
          play(game)
          repo game game.id must beSome.like {
            case g ⇒ addNewLines(g.toChess.board.visual) must_== """
  kr  nr
p  n ppp
B p p

 P P B
  N    P
 PPK PP
       q
"""
          }
        }
      }
    }
  }
}
