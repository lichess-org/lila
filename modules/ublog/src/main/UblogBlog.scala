package lila.ublog

import lila.user.User

case class UblogBlog(
    _id: UblogBlog.Id,
    title: Option[String],
    intro: Option[String],
    tier: Int,           // actual tier, auto or set by a mod
    modTier: Option[Int] // tier set by a mod
) {
  def id      = _id
  def visible = tier >= UblogBlog.Tier.VISIBLE
  def mashup  = tier >= UblogBlog.Tier.LOW
}

object UblogBlog {

  sealed abstract class Id(val full: String)
  object Id {
    private val sep = ':'
    case class User(id: String) extends Id(s"user$sep$id")
    def apply(full: String): Option[Id] = full split sep match {
      case Array("user", id) => User(id).some
      case _                 => none
    }
  }

  object Tier {
    val HIDDEN  = 0 // not visible
    val VISIBLE = 1 // not listed in community page
    val LOW     = 2 // from here, ranking boost
    val NORMAL  = 3
    val HIGH    = 4
    val BEST    = 5

    def default(user: User) =
      if (user.marks.troll) Tier.HIDDEN
      else if (user.hasTitle || user.perfs.standard.glicko.establishedIntRating.exists(_ > 2200)) Tier.NORMAL
      else Tier.LOW
  }

  def make(user: User) = UblogBlog(
    _id = Id.User(user.id),
    title = none,
    intro = none,
    tier = Tier default user,
    modTier = none
  )
}
