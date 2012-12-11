package lila
package team

import user.User

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import java.text.Normalizer
import scalaz.effects._

case class Team(
    @Key("_id") id: String, // also the url slug
    name: String,
    location: Option[String],
    description: String,
    nbMembers: Int,
    enabled: Boolean,
    createdAt: DateTime,
    createdBy: String) {

  def slug = id

  def disabled = !enabled
}

object Team {

  def apply(
    name: String,
    location: Option[String],
    description: String,
    createdBy: User): Team = new Team(
    id = nameToId(name),
    name = name,
    location = location,
    description = description,
    nbMembers = 1,
    enabled = true,
    createdAt = DateTime.now,
    createdBy = createdBy.id)

  def nameToId(name: String) = templating.StringHelper slugify name
}
