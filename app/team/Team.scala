package lila
package team

import user.User

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import java.text.Normalizer

case class Team(
    @Key("_id") id: String, // also the url slug
    name: String,
    location: Option[String],
    description: String,
    members: List[Member],
    nbMembers: Int,
    enabled: Boolean,
    createdAt: DateTime,
    createdBy: String) {

  def slug = id

  def contains(userId: String): Boolean = members exists (_ is userId) 
  def contains(user: User): Boolean = contains(user.id)
  
  def canJoin(user: User) = true

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
    members = Member(createdBy) :: Nil,
    nbMembers = 1,
    enabled = true,
    createdAt = DateTime.now,
    createdBy = createdBy.id)

    def nameToId(name: String) = templating.StringHelper slugify name
}
