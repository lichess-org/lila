package lila.ublog

import lila.core.perm.Granter

import lila.core.perf.UserWithPerfs

case class UblogBlog(
    _id: UblogBlog.Id,
    tier: UblogRank.Tier,           // actual tier, auto or set by a mod
    modTier: Option[UblogRank.Tier] // tier set by a mod
):
  inline def id = _id
  def visible   = tier >= UblogRank.Tier.VISIBLE
  def listed    = tier >= UblogRank.Tier.LOW

  def userId = id match
    case UblogBlog.Id.User(userId) => userId

  def allows = UblogBlog.Allows(userId)

object UblogBlog:

  enum Id(val full: String):
    case User(id: UserId) extends Id(s"user${Id.sep}$id")
  object Id:
    private val sep = ':'
    def apply(full: String): Option[Id] = full.split(sep) match
      case Array("user", id) => User(UserId(id)).some
      case _                 => none

  def make(user: UserWithPerfs) = UblogBlog(
    _id = Id.User(user.id),
    tier = UblogRank.Tier.default(user),
    modTier = none
  )

  class Allows(creator: UserId):
    def moderate(using Option[Me]): Boolean = Granter.opt[Me](_.ModerateBlog)
    def edit(using me: Option[Me]): Boolean = me.exists(creator.is(_)) || moderate
    def create(using me: Option[Me]): Boolean =
      edit || (creator.is(UserId.lichess) && Granter.opt[Me](_.Pages))
    def draft(using me: Option[Me]): Boolean =
      create || (Granter.opt[Me](_.LichessTeam) && creator.is(UserId.lichess))
