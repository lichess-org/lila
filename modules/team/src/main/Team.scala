package lila.team

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

case class Team(
    _id: String, // also the url slug
    name: String,
    location: Option[String],
    description: String,
    nbMembers: Int,
    enabled: Boolean,
    open: Boolean,
    createdAt: DateTime,
    createdBy: String) {

  def id = _id

  def slug = id

  def disabled = !enabled

  def isCreator(user: String) = user == createdBy
}

object Team {

  def make(
    name: String,
    location: Option[String],
    description: String,
    open: Boolean,
    createdBy: User): Team = new Team(
    _id = nameToId(name),
    name = name,
    location = location,
    description = description,
    nbMembers = 1,
    enabled = true,
    open = open,
    createdAt = DateTime.now,
    createdBy = createdBy.id)

  def nameToId(name: String) = (lila.common.String slugify name) |> { slug =>
    // if most chars are not latin, go for random slug
    (slug.size > (name.size / 2)).fold(slug, Random nextStringUppercase 8)
  }
}
