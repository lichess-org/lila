package lila.app
package forum

import com.novus.salat.annotations.Key

case class Categ(
    @Key("_id") slug: String,
    name: String,
    desc: String,
    pos: Int,
    team: Option[String] = None,
    nbTopics: Int = 0,
    nbPosts: Int = 0,
    lastPostId: String = "") {

  def isStaff = slug == "staff"

  def isTeam = team.nonEmpty

  def id = slug
}
