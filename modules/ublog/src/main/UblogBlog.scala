package lila.ublog

import lila.user.User

case class UblogBlog(
    _id: UblogBlog.Id,
    tier: UblogBlog.Tier,           // actual tier, auto or set by a mod
    modTier: Option[UblogBlog.Tier] // tier set by a mod
):
  inline def id = _id
  def visible   = tier >= UblogBlog.Tier.VISIBLE
  def listed    = tier >= UblogBlog.Tier.LOW

  def userId = id match
    case UblogBlog.Id.User(userId) => userId

object UblogBlog:

  enum Id(val full: String):
    case User(id: UserId) extends Id(s"user${Id.sep}$id")
  object Id:
    private val sep = ':'
    def apply(full: String): Option[Id] = full split sep match
      case Array("user", id) => User(UserId(id)).some
      case _                 => none

  opaque type Tier = Int
  object Tier extends OpaqueInt[Tier]:
    val HIDDEN: Tier  = 0 // not visible
    val VISIBLE: Tier = 1 // not listed in community page
    val LOW: Tier     = 2 // from here, ranking boost
    val NORMAL: Tier  = 3
    val HIGH: Tier    = 4
    val BEST: Tier    = 5

    def default(user: User.WithPerfs) =
      if user.marks.troll then Tier.HIDDEN
      else if user.hasTitle || user.perfs.standard.glicko.establishedIntRating.exists(_ > 2200)
      then Tier.NORMAL
      else Tier.LOW

    val options = List(
      HIDDEN  -> "Hidden",
      VISIBLE -> "Unlisted",
      LOW     -> "Low tier",
      NORMAL  -> "Normal tier",
      HIGH    -> "High tier",
      BEST    -> "Best tier"
    )
    def name(tier: Tier) = options.collectFirst {
      case (t, n) if t == tier => n
    } | "???"

  def make(user: User.WithPerfs) = UblogBlog(
    _id = Id.User(user.id),
    tier = Tier default user,
    modTier = none
  )
