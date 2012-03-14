package lila.system

import lila.chess._
import scalaz.{ Success, Failure }
import scalaz.effects._
import com.mongodb.casbah.Imports._

class GameRepoTest extends SystemTest {

  val env = SystemEnv()
  val repo = env.gameRepo
  val anyGame = repo.findOne(DBObject()) flatMap repo.decode get // unsafe but who cares

  "the game repo" should {
    "find a game" in {
      "by ID" in {
        "non existing" in {
          repo game "haha" must beIO.failure
        }
        "existing" in {
          repo game anyGame.id must beIO.like {
            case g ⇒ g.id must_== anyGame.id
          }
        }
      }
    }
    "find a player" in {
      "by private ID" in {
        "non existing" in {
          repo player "huhu" must beIO.failure
        }
        "existing" in {
          val player = anyGame.players.head
          anyGame fullIdOf player map repo.player must beSome.like {
            case iop ⇒ iop must beIO.like {
              case (g, p) ⇒ p.id must_== player.id
            }
          }
        }
      }
      "by ID and color" in {
        "non existing" in {
          repo.player("haha", White) must beIO.failure
        }
        "existing" in {
          val player = anyGame.players.head
          repo.player(anyGame.id, player.color) must beIO.like {
            case (g, p) ⇒ p.id must_== player.id
          }
        }
      }
    }
    "insert a new game" in {
      val game = newDbGameWithRandomIds()
      "find the saved game" in {
        (for {
          _ ← repo insert game
          newGame ← repo game game.id
        } yield newGame) must beIO.like {
          case g ⇒ g must_== game
        }
      }
    }
    "update a game" in {
      val game = newDbGameWithRandomIds()
      val updated = game.copy(turns = game.turns + 1)
      "find the updated game" in {
        (for {
          _ ← repo insert game
          _ ← repo save updated
          newGame ← repo game game.id
        } yield newGame) must beIO.like {
          case g ⇒ g must_== updated
        }
      }
    }
  }
}
