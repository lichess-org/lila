package lila.system

import lila.chess._
import model._

class ServerTest extends SystemTest {

  val env = SystemEnv()
  val repo = env.gameRepo
  val server = env.server

  def insert(dbGame: DbGame = newDbGameWithRandomIds) = {
    repo insert dbGame
    dbGame
  }
  def move(game: DbGame, m: String = "d2 d4") = server.playMove(game fullIdOf White, m)

  "the server" should {
    "play a single move" in {
      "wrong player" in {
        val game = insert()
        move(game, "d7 d5") must beFailure
      }
      "report success" in {
        val game = insert()
        move(game) must beSuccess
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

      def play(game: DbGame) = for (m ← moves) yield move(game, m)

      "report success" in {
        val game = insert()
        sequenceValid(play(game)) must beSuccess
      }
      "be persisted" in {
        val game = insert()
        play(game)
        val found = repo game game.id
        "update turns" in {
          found must beSome.like {
            case g ⇒ g.turns must_== 27
          }
        }
        "update board" in {
          found must beSome.like {
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
        "event stacks" in {
          val stack = found map (_ player White) map (_.eventStack)
          "high version number" in {
            stack must beSome.like { case s ⇒ s.version must be_>(20) }
          }
          "rotated" in {
            stack must beSome.like { case s ⇒ s.events.size must_== 16 }
          }
        }
      }
    }
    "play to threefold repetition" in {
      val moves = List("b1 c3", "b8 c6", "c3 b1", "c6 b8", "b1 c3", "b8 c6", "c3 b1", "c6 b8", "b1 c3", "b8 c6")

      def play(game: DbGame) = for (m ← moves) yield move(game, m)

      "report success" in {
        val game = insert()
        sequenceValid(play(game)) must beSuccess
      }
      "be persisted" in {
        val game = insert()
        play(game)
        val found = repo game game.id
        val events = found map (_ player White) map (_.eventStack.events)
        "propose threefold" in {
          events must beSome.like {
            case es ⇒ es map (_._2) must contain(ThreefoldEvent())
          }
        }
      }
    }
    "play on playing game" in {
      val dbGame = insert(randomizeIds(newDbGameWithBoard("""
PP kr
K
""")))
      move(dbGame, "a1 b1") must beSuccess
    }
    "play on finished game" in {
      "by checkmate" in {
        val game = insert(randomizeIds(newDbGameWithBoard("""
PP
K  r
""")))
        move(game, "a1 b1") must beFailure
      }
      "by autodraw" in {
        val game = insert(randomizeIds(newDbGameWithBoard("""
      k
K     B""")))
        move(game, "a1 b1") must beFailure
      }
    }
  }
}
