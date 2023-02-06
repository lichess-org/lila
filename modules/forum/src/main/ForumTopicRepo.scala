package lila.forum

import Filter.*
import lila.db.dsl.{ *, given }
import lila.user.User

final private class ForumTopicRepo(val coll: Coll, filter: Filter = Safe)(using
    Executor
):

  import BSONHandlers.given

  def forUser(user: Option[User]) =
    withFilter(user.filter(_.marks.troll).fold[Filter](Safe) { u =>
      SafeAnd(u.id)
    })
  def withFilter(f: Filter) = if (f == filter) this else new ForumTopicRepo(coll, f)
  def unsafe                = withFilter(Unsafe)

  private val noTroll = $doc("troll" -> false)
  private val trollFilter = filter match
    case Safe       => noTroll
    case SafeAnd(u) => $or(noTroll, $doc("userId" -> u))
    case Unsafe     => $empty

  private lazy val notStickyQuery = $doc("sticky" $ne true)
  private lazy val stickyQuery    = $doc("sticky" -> true)

  def byId(id: ForumTopicId): Fu[Option[ForumTopic]] = coll.byId[ForumTopic](id)

  def close(id: ForumTopicId, value: Boolean): Funit =
    coll.updateField($id(id), "closed", value).void

  def remove(topic: ForumTopic): Funit =
    coll.delete.one($id(topic.id)).void

  def sticky(id: ForumTopicId, value: Boolean): Funit =
    coll.updateField($id(id), "sticky", value).void

  def byCateg(categ: ForumCateg): Fu[List[ForumTopic]] =
    coll.list[ForumTopic](byCategQuery(categ))

  def countByCateg(categ: ForumCateg): Fu[Int] =
    coll.countSel(byCategQuery(categ))

  def byTree(categId: ForumCategId, slug: String): Fu[Option[ForumTopic]] =
    coll.one[ForumTopic]($doc("categId" -> categId, "slug" -> slug) ++ trollFilter)

  def existsByTree(categId: ForumCategId, slug: String): Fu[Boolean] =
    coll.exists($doc("categId" -> categId, "slug" -> slug))

  def stickyByCateg(categ: ForumCateg): Fu[List[ForumTopic]] =
    coll.list[ForumTopic](byCategQuery(categ) ++ stickyQuery)

  def nextSlug(categ: ForumCateg, name: String, it: Int = 1): Fu[String] =
    val slug = ForumTopic.nameToId(name) + ~(it != 1).option("-" + it)
    // also take troll topic into accounts
    unsafe.byTree(categ.id, slug) flatMap { found =>
      if (found.isDefined) nextSlug(categ, name, it + 1)
      else fuccess(slug)
    }

  def byCategQuery(categ: ForumCateg)          = $doc("categId" -> categ.slug) ++ trollFilter
  def byCategNotStickyQuery(categ: ForumCateg) = byCategQuery(categ) ++ notStickyQuery
