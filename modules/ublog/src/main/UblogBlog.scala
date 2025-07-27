package lila.ublog

import lila.core.perm.Granter

case class UblogBlog(
    _id: UblogBlog.Id,
    tier: UblogBlog.Tier, // actual tier, auto or set by a mod
    modTier: Option[UblogBlog.Tier], // tier set by a mod
    modNote: Option[String]
):
  inline def id = _id
  def visible = tier >= UblogBlog.Tier.UNLISTED
  def listed = tier >= UblogBlog.Tier.LOW

  def userId = id match
    case UblogBlog.Id.User(userId) => userId

  def allows = UblogBlog.Allows(userId)

object UblogBlog:
  opaque type Tier = Int
  object Tier extends RelaxedOpaqueInt[Tier]:
    val HIDDEN: Tier = 0
    val UNLISTED: Tier = 1
    val LOW: Tier = 2
    val NORMAL: Tier = 3
    val HIGH: Tier = 4
    val BEST: Tier = 5

    def default(user: User) =
      if user.marks.troll then Tier.HIDDEN
      else Tier.LOW

    val options = List(
      HIDDEN -> "Hidden",
      UNLISTED -> "Unlisted",
      LOW -> "Low",
      NORMAL -> "Normal",
      HIGH -> "High",
      BEST -> "Best"
    )

    def name(tier: Tier) = options.collectFirst {
      case (t, n) if t == tier => n
    } | "???"

  enum Id(val full: String):
    case User(id: UserId) extends Id(s"user${Id.sep}$id")
  object Id:
    private val sep = ':'
    def apply(full: String): Option[Id] = full.split(sep) match
      case Array("user", id) => User(UserId(id)).some
      case _ => none

  def make(user: User) = UblogBlog(
    _id = Id.User(user.id),
    tier = UblogBlog.Tier.default(user),
    modTier = none,
    modNote = none
  )

  class Allows(creator: UserId):
    def moderate(using Option[Me]): Boolean = Granter.opt(_.ModerateBlog)
    def edit(using me: Option[Me]): Boolean =
      me.exists(creator.is(_)) ||
        (creator.is(UserId.lichess) && Granter.opt(_.Pages)) ||
        moderate
    def draft(using me: Option[Me]): Boolean =
      edit || (creator.is(UserId.lichess) && Granter.opt(_.LichessTeam))
