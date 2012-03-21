package lila.system

import scalaz.{ Success, Failure }
import scalaz.effects._
import com.mongodb.casbah.Imports._

class HookRepoTest extends SystemTest {
  sequential

  val env = SystemEnv()
  val repo = env.hookRepo

  "find no hook" in {
    repo.remove(DBObject())
    repo.allOpen must beIO.like {
      case hooks ⇒ hooks must beEmpty
    }
  }
}
