package lila.bookmark

import lila.db.Implicits._
import lila.db.DbApi._
import lila.db.test.WithDb

import org.specs2.mutable.Specification

import play.api.test._
import play.api.libs.json._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

final class RepoTest extends Specification {

  val user = "thibault"
  val game = "abcdefgh"

  implicit val timeout = makeTimeout.large

  def cleanRepo = Env.current.bookmarkRepo ~ { repo =>
    (repo.remove(select.all)/* >> repo.insert(bookmark) */).await
  }

  "The bookmark repo" should {

    "toggle on" in new WithDb {
      lazy val repo = cleanRepo
      (repo.toggle(game, user) >> repo.userIdsByGameId(game)).await must_== List(user)
    }
  }
}
