package lila.study

import scalalib.paginator.Paginator

import lila.core.i18n.I18nKey
import lila.core.study.Order
import lila.db.dsl.{ *, given }
import lila.db.paginator.{ Adapter, CachedAdapter }

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using Executor):

  val maxPerPage = MaxPerPage(16)
  val defaultNbChaptersPerStudy = 4

  import BSONHandlers.given
  import studyRepo.{
    selectLiker,
    selectMemberId,
    selectOwnerId,
    selectPrivateOrUnlisted,
    selectPublic,
    selectTopic
  }

  def all(order: Order, page: Int)(using me: Option[Me]) =
    paginator(
      noRelaySelect ++ accessSelect,
      order,
      page,
      fuccess(9999).some
    )

  def byOwner(owner: User, order: Order, page: Int)(using me: Option[Me]) =
    paginator(
      selectOwnerId(owner.id) ++ accessSelect,
      order,
      page
    )

  def mine(order: Order, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me),
      order,
      page
    )

  def minePublic(order: Order, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me) ++ selectPublic,
      order,
      page
    )

  def minePrivate(order: Order, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me) ++ selectPrivateOrUnlisted,
      order,
      page
    )

  def mineMember(order: Order, page: Int)(using me: Me) =
    paginator(
      selectMemberId(me) ++ $doc("ownerId".$ne(me.userId)),
      order,
      page
    )

  def mineLikes(order: Order, page: Int)(using me: Me) =
    paginator(
      selectLiker(me) ++ accessSelect ++ $doc("ownerId".$ne(me.userId)),
      order,
      page
    )

  def byTopic(topic: StudyTopic, order: Order, page: Int)(using me: Option[Me]) =
    val onlyMine = me.ifTrue(order == Order.mine)
    paginator(
      selectTopic(topic) ++ onlyMine.fold(accessSelect)(selectMemberId(_)),
      order,
      page,
      hint = onlyMine.isDefined.option($doc("uids" -> 1, "rank" -> -1))
    )

  private def accessSelect(using me: Option[Me]) =
    me.fold(selectPublic): u =>
      $or(selectPublic, selectMemberId(u))

  private val noRelaySelect = $doc("from".$ne("relay"))

  private def paginator(
      selector: Bdoc,
      order: Order,
      page: Int,
      nbResults: Option[Fu[Int]] = none,
      hint: Option[Bdoc] = none
  )(using me: Option[Me]): Fu[Paginator[Study.WithChaptersAndLiked]] = studyRepo.coll: coll =>
    val adapter = Adapter[Study](
      collection = coll,
      selector = selector,
      projection = studyRepo.projection.some,
      sort = order match
        case Order.hot => $sort.desc("rank")
        case Order.newest => $sort.desc("createdAt")
        case Order.oldest => $sort.asc("createdAt")
        case Order.updated => $sort.desc("updatedAt")
        case Order.popular => $sort.desc("likes")
        case Order.alphabetical => $sort.asc("name")
        // mine filter for topic view
        case Order.mine => $sort.desc("rank")
      ,
      hint = hint
    ).mapFutureList(withChaptersAndLiking())
    Paginator(
      adapter = nbResults.fold(adapter): nb =>
        CachedAdapter(adapter, nb),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def withChaptersAndLiking(
      nbChaptersPerStudy: Int = defaultNbChaptersPerStudy
  )(studies: Seq[Study])(using me: Option[Me]): Fu[Seq[Study.WithChaptersAndLiked]] =
    withChapters(studies, nbChaptersPerStudy).flatMap(withLiking)

  private def withChapters(
      studies: Seq[Study],
      nbChaptersPerStudy: Int
  ): Fu[Seq[Study.WithChapters]] =
    chapterRepo.idNamesByStudyIds(studies.map(_.id), nbChaptersPerStudy).map { chapters =>
      studies.map { study =>
        Study.WithChapters(study, (chapters.get(study.id)).so(_.map(_.name)))
      }
    }

  private def withLiking(
      studies: Seq[Study.WithChapters]
  )(using me: Option[Me]): Fu[Seq[Study.WithChaptersAndLiked]] =
    me.so: u =>
      studyRepo.filterLiked(u, studies.map(_.study.id))
    .map: liked =>
        studies.map { case Study.WithChapters(study, chapters) =>
          Study.WithChaptersAndLiked(study, chapters, liked(study.id))
        }

object Orders:
  import lila.core.study.Order
  val default = Order.hot
  val list = Order.values.toList
  val withoutMine = list.filterNot(_ == Order.mine)
  val withoutSelector = withoutMine.filter(o => o != Order.oldest && o != Order.alphabetical)
  private val byKey = list.mapBy(_.toString)
  def apply(key: String): Order = byKey.getOrElse(key, default)
  val name: Order => I18nKey =
    case Order.hot => I18nKey.study.hot
    case Order.newest => I18nKey.study.dateAddedNewest
    case Order.oldest => I18nKey.study.dateAddedOldest
    case Order.updated => I18nKey.study.recentlyUpdated
    case Order.popular => I18nKey.study.mostPopular
    case Order.alphabetical => I18nKey.study.alphabetical
    case Order.mine => I18nKey.study.myStudies
