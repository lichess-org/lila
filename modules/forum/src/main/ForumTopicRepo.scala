package lila.forum

import lila.core.forum.ForumTopicMini
import lila.db.dsl.{ *, given }
import lila.core.id.ForumTopicSlug

import Filter.*

final private class ForumTopicRepo(val coll: Coll, filter: Filter = Safe)(using
    Executor
):

  import BSONHandlers.given

  def forUser(user: Option[User]) =
    withFilter(user.filter(_.marks.troll).fold[Filter](Safe) { u =>
      SafeAnd(u.id)
    })
  def withFilter(f: Filter) = if f == filter then this else new ForumTopicRepo(coll, f)
  def unsafe = withFilter(Unsafe)

  private val noTroll = bdoc("troll" -> false)
  private val trollFilter = filter match
    case Safe => noTroll
    case SafeAnd(u) => $or(noTroll, bdoc("userId" -> u))
    case Unsafe => emptyBdoc

  def byId(id: ForumTopicId): Fu[Option[ForumTopic]] = coll.byId[ForumTopic](id)

  def byIds(ids: Seq[ForumTopicId]): Fu[List[ForumTopicMini]] =
    coll.byStringIds(ForumTopicId.raw(ids))

  def close(id: ForumTopicId, value: Boolean, byMod: Boolean): Funit =
    coll.update
      .one(bid(id), $set("closed" -> value, "closedByMod" -> (value && byMod)))
      .void

  def closedByMod(id: ForumTopicId): Fu[Boolean] = coll.exists(bid(id) ++ bdoc("closedByMod" -> true))

  def remove(topic: ForumTopic): Funit =
    coll.delete.one(bid(topic.id)).void

  def sticky(id: ForumTopicId, value: Option[UserId]): Funit =
    coll.updateOrUnsetField(bid(id), "sticky", value).void

  def byCateg(categ: ForumCategId): Fu[List[ForumTopic]] =
    coll.list(byCategQuery(categ))

  def countByCateg(categ: ForumCategId): Fu[Int] =
    coll.countSel(byCategQuery(categ))

  def byTree(categId: ForumCategId, slug: ForumTopicSlug): Fu[Option[ForumTopic]] =
    coll.one(bdoc("categId" -> categId, "slug" -> slug) ++ trollFilter)

  def existsByTree(categId: ForumCategId, slug: ForumTopicSlug): Fu[Boolean] =
    coll.exists(bdoc("categId" -> categId, "slug" -> slug))

  private[forum] def stickyByCateg(categ: ForumCategId): Fu[List[ForumTopic]] =
    coll.list(byCategQuery(categ) ++ "sticky".$exists(true))

  def nextSlug(categ: ForumCateg, name: String, it: Int = 1): Fu[ForumTopicSlug] =
    val slug = ForumTopicSlug:
      ForumTopic.nameToId(name) + ~(it != 1).option("-" + it)
    // also take troll topic into accounts
    unsafe.byTree(categ.id, slug).flatMap { found =>
      if found.isDefined then nextSlug(categ, name, it + 1)
      else fuccess(slug)
    }

  def byCategQuery(categ: ForumCategId) = bdoc("categId" -> categ) ++ trollFilter
  def byCategNotStickyQuery(categ: ForumCategId) = byCategQuery(categ) ++ "sticky".$exists(false)
