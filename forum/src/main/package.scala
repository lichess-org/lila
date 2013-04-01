package lila

import lila.db.Tube
import Tube.Helpers._
import play.api.libs.json._

package object forum extends PackageObject with WithPlay {

  private val categDefaults = Json.obj(
    "team" -> none[String],
    "nbTopics" -> 0,
    "nbPosts" -> 0,
    "lastPostId" -> "")

  private[forum] lazy val categTube = Tube(
    reader = (__.json update merge(categDefaults)) andThen Json.reads[Categ],
    writer = Json.writes[Categ]
  ) inColl Env.current.categColl

  private val topicDefaults = Json.obj(
    "nbPosts" -> 0,
    "lastPostId" -> "")

  private[forum] lazy val topicTube = Tube(
    reader = (__.json update (
      merge(topicDefaults) andThen readDate('createdAt) andThen readDate('updatedAt)
    )) andThen Json.reads[Topic],
    writer = Json.writes[Topic],
    writeTransformer = (__.json update (
      writeDate('createdAt) andThen readDate('updatedAt)
    )).some
  ) inColl Env.current.topicColl

  private[forum] lazy val postTube = Tube(
    reader = (__.json update readDate('createdAt)) andThen Json.reads[Post],
    writer = Json.writes[Post],
    writeTransformer = (__.json update writeDate('createdAt)).some
  ) inColl Env.current.postColl

  private[forum] object allTubes {

    implicit def postT = postTube
    implicit def topicT = topicTube
    implicit def categT = categTube
  }

  private[forum] def teamSlug(id: String) = "team-" + id
}
