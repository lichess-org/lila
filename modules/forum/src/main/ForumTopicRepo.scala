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

  private val noTroll = $doc("troll" -> false)
  private val trollFilter = filter match
    case Safe => noTroll
    case SafeAnd(u) => $or(noTroll, $doc("userId" -> u))
    case Unsafe => $empty

  private lazy val notStickyQuery = $or($doc("sticky".$exists(false)), $doc("sticky" -> false))
  private lazy val stickyQuery = $and($doc("sticky".$exists(true)), $doc("sticky".$ne(false)))

  def byId(id: ForumTopicId): Fu[Option[ForumTopic]] = coll.byId[ForumTopic](id)

  def byIds(ids: Seq[ForumTopicId]): Fu[List[ForumTopicMini]] =
    coll.byStringIds(ForumTopicId.raw(ids))

  def close(id: ForumTopicId, value: Boolean, byMod: Boolean): Funit =
    coll.update
      .one($id(id), $set("closed" -> value, "closedByMod" -> (value && byMod)))
      .void

  def closedByMod(id: ForumTopicId): Fu[Boolean] = coll.exists($id(id) ++ $doc("closedByMod" -> true))

  def remove(topic: ForumTopic): Funit =
    coll.delete.one($id(topic.id)).void

  def sticky(id: ForumTopicId, value: Boolean): Funit =
    coll.updateField($id(id), "sticky", value).void

  def sticky(id: ForumTopicId, value: ForumTopic.Sticky): Funit =
    coll.updateField($id(id), "sticky", value).void

  def byCateg(categ: ForumCategId): Fu[List[ForumTopic]] =
    coll.list(byCategQuery(categ))

  def countByCateg(categ: ForumCategId): Fu[Int] =
    coll.countSel(byCategQuery(categ))

  def byTree(categId: ForumCategId, slug: ForumTopicSlug): Fu[Option[ForumTopic]] =
    coll.one($doc("categId" -> categId, "slug" -> slug) ++ trollFilter)

  def existsByTree(categId: ForumCategId, slug: ForumTopicSlug): Fu[Boolean] =
    coll.exists($doc("categId" -> categId, "slug" -> slug))

  def stickyByCateg(categ: ForumCategId): Fu[List[ForumTopic]] =
    coll.list(byCategQuery(categ) ++ stickyQuery)

  def nextSlug(categ: ForumCateg, name: String, it: Int = 1): Fu[ForumTopicSlug] =
    val slug = ForumTopicSlug:
      ForumTopic.nameToId(name) + ~(it != 1).option("-" + it)
    // also take troll topic into accounts
    unsafe.byTree(categ.id, slug).flatMap { found =>
      if found.isDefined then nextSlug(categ, name, it + 1)
      else fuccess(slug)
    }

  def byCategQuery(categ: ForumCategId) = $doc("categId" -> categ) ++ trollFilter
  def byCategNotStickyQuery(categ: ForumCategId) = byCategQuery(categ) ++ notStickyQuery
