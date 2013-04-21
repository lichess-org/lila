package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object forum extends PackageObject with WithPlay {

  object tube {

    private[forum] implicit lazy val categTube =
      Categ.tube inColl Env.current.categColl

    private[forum] implicit lazy val topicTube =
      Topic.tube inColl Env.current.topicColl

    implicit lazy val postTube =
      Post.tube inColl Env.current.postColl
  }

  private[forum] def teamSlug(id: String) = "team-" + id
}
