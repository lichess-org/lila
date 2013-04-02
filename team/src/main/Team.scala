package lila.team

import lila.user.User

import ornicar.scalalib.Random
import org.joda.time.DateTime

case class Team(
    id: String, // also the url slug
    name: String,
    location: Option[String],
    description: String,
    nbMembers: Int,
    enabled: Boolean,
    open: Boolean,
    createdAt: DateTime,
    createdBy: String) {

  def slug = id

  def disabled = !enabled

  def isCreator(user: String) = user == createdBy
}

object Teams {

  def apply(
    name: String,
    location: Option[String],
    description: String,
    open: Boolean,
    createdBy: User): Team = new Team(
    id = nameToId(name),
    name = name,
    location = location,
    description = description,
    nbMembers = 1,
    enabled = true,
    open = open,
    createdAt = DateTime.now,
    createdBy = createdBy.id)

  def nameToId(name: String) = (lila.common.String slugify name) |> { slug ⇒
    // if most chars are not latin, go for random slug
    (slug.size > (name.size / 2)).fold(slug, Random nextString 8)
  }

  import lila.db.Tube, Tube.Helpers._
  import play.api.libs.json._

  val tube = Tube(
    reader = (__.json update readDate('createdAt)) andThen Json.reads[Team],
    writer = Json.writes[Team],
    writeTransformer = (__.json update writeDate('createdAt)).some
  ) 
}
