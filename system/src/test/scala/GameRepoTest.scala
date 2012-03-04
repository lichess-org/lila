package lila.system

import lila.chess._

class GameRepoTest extends SystemTest {

  val env = SystemEnv()
  val repo = env.gameRepo
  val anyGame = repo.anyGame.get

  "the game repo" should {
    "find a game" in {
      "by ID" in {
        "non existing" in {
          repo game "haha" must beNone
        }
        "existing" in {
          repo game anyGame.id must beSome.like {
            case g ⇒ g.id must_== anyGame.id
          }
        }
      }
    }
    "find a player" in {
      "by private ID" in {
        "non existing" in {
          repo player "huhu" must be none
        }
        "existing" in {
          val player = anyGame.players.head
          anyGame fullIdOf player flatMap repo.player must beSome.like {
            case (g, p) ⇒ p.id must_== player.id
          }
        }
      }
      "by ID and color" in {
        "non existing" in {
          repo.player("haha", White) must beNone
        }
        "existing" in {
          val player = anyGame.players.head
          repo.player(anyGame.id, player.color) must beSome.like {
            case (g, p) ⇒ p.id must_== player.id
          }
        }
      }
    }
    "insert a new game" in {
      sequential
      val game = newDbGameWithRandomIds()
      repo insert game
      "find the saved game" in {
        repo game game.id must_== Some(game)
      }
    }
    "update a game" in {
      sequential
      val game = newDbGameWithRandomIds()
      repo insert game
      val updated = game.copy(turns = game.turns + 1)
      repo save updated
      "find the updated game" in {
        repo game updated.id must_== Some(updated)
      }
    }
  }
}
