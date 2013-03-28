package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object message extends PackageObject with WithPlay {

  implicit lazy val postTube = Tube(
    reader = (__.json update (
      readDate('createdAt)
    )) andThen Json.reads[Post],
    writer = Json.writes[Post],
    writeTransformer = (__.json update (
      writeDate('createdAt)
    )).some
  )(Env.current.postRepo.coll)

  implicit lazy val threadTube = Tube(
    reader = (__.json update (
      readDate('createdAt) andThen readDate('updatedAt)
    )) andThen Json.reads[Thread],
    writer = Json.writes[Thread],
    writeTransformer = (__.json update (
      writeDate('createdAt) andThen writeDate('updatedAt)
    )).some
  )(Env.current.threadRepo.coll)
}
