package lila.bookmark

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import org.specs2.mutable.Specification
import play.api.libs.json._
import play.api.test._

import lila.db.api._
import lila.db.Implicits._
import lila.db.test.WithDb
import tube.bookmarkTube

final class RepoTest extends Specification {

  val user = "thibault"
  val game = "abcdefgh"

  import makeTimeout.large

  def cleanRepo = 
    ($remove($select.all) inject BookmarkRepo /* >> repo.insert(bookmark) */ ).await

  "The bookmark repo" should {

    // "toggle on" in new WithDb {
    //   lazy val repo = cleanRepo
    //   (repo.toggle(game, user) >>
    //     repo.userIdsByGameId(game)).await must_== List(user)
    // }

    "toggle on and off" in new WithDb {
      lazy val repo = cleanRepo
      (repo.toggle(game, user) >>
        repo.toggle(game, user) >>
        repo.userIdsByGameId(game)).await must_== Nil
    }
  }
}
