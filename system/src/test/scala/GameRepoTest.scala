package lila.system

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
  }
}
