package lila
package forum

import com.novus.salat.annotations.Key

case class Categ(
    @Key("_id") slug: String,
    name: String,
    desc: String,
    pos: Int,
    nbTopics: Int = 0,
    nbPosts: Int = 0,
    lastPostId: String = "") {

  def isStaff = slug == "staff"
}
