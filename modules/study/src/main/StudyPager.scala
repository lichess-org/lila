package lila.study

import scalalib.paginator.Paginator

import lila.core.i18n.I18nKey
import lila.core.study.StudyOrder
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
    selectUnlisted,
    selectTopic
  }

  def all(order: StudyOrder, page: Int)(using me: Option[Me]) =
    paginator(
      noRelaySelect ++ accessSelect(),
      order,
      page,
      fuccess(9999).some
    )

  def byOwner(owner: User, order: StudyOrder, page: Int)(using me: Option[Me]) =
    paginator(
      selectOwnerId(owner.id) ++ accessSelect(),
      order,
      page
    )

  def mine(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me),
      order,
      page
    )

  def minePublic(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me) ++ selectPublic,
      order,
      page
    )

  def minePrivate(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me) ++ selectPrivateOrUnlisted,
      order,
      page
    )

  def mineMember(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectMemberId(me) ++ $doc("ownerId".$ne(me.userId)),
      order,
      page
    )

  def mineLikes(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectLiker(me) ++ accessSelect(true) ++ $doc("ownerId".$ne(me.userId)),
      order,
      page
    )

  def byTopic(topic: StudyTopic, order: StudyOrder, page: Int)(using me: Option[Me]) =
    val onlyMine = me.ifTrue(order == StudyOrder.mine)
    paginator(
      selectTopic(topic) ++ onlyMine.fold(accessSelect())(selectMemberId(_)),
      order,
      page,
      hint = onlyMine.isDefined.option($doc("uids" -> 1, "rank" -> -1))
    )

  private def accessSelect(includeUnlisted: Boolean = false)(using me: Option[Me]) =
    me.fold(selectPublic): u =>
      if includeUnlisted then $or(selectPublic, selectMemberId(u), selectUnlisted)
      else $or(selectPublic, selectMemberId(u))

  private val noRelaySelect = $doc("from".$ne("relay"))

  private def paginator(
      selector: Bdoc,
      order: StudyOrder,
      page: Int,
      nbResults: Option[Fu[Int]] = none,
      hint: Option[Bdoc] = none
  )(using Option[Me]): Fu[Paginator[Study.WithChaptersAndLiked]] = studyRepo.coll: coll =>
    val adapter = Adapter[Study](
      collection = coll,
      selector = selector,
      projection = studyRepo.projection.some,
      sort = order match
        case StudyOrder.hot => $sort.desc("rank")
        case StudyOrder.newest => $sort.desc("createdAt")
        case StudyOrder.oldest => $sort.asc("createdAt")
        case StudyOrder.updated => $sort.desc("updatedAt")
        case StudyOrder.popular => $sort.desc("likes")
        case StudyOrder.alphabetical => $sort.asc("name")
        // mine filter for topic view
        case StudyOrder.mine => $sort.desc("rank")
        // relevant not used here
        case StudyOrder.relevant => $sort.desc("rank")
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
    for chapters <- chapterRepo.idNamesByStudyIds(studies.map(_.id), nbChaptersPerStudy)
    yield studies.map: study =>
      Study.WithChapters(study, chapters.get(study.id).so(_.map(_.name)))

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
  import lila.core.study.StudyOrder
  val default = StudyOrder.hot
  val list = StudyOrder.all.filter(_ != StudyOrder.relevant)
  val withoutMine = list.filterNot(_ == StudyOrder.mine)
  val withoutSelector = withoutMine.filter(o => o != StudyOrder.oldest && o != StudyOrder.alphabetical)
  val search =
    List(StudyOrder.hot, StudyOrder.newest, StudyOrder.popular, StudyOrder.alphabetical, StudyOrder.relevant)
  private val byKey = list.mapBy(_.toString)
  def apply(key: String): StudyOrder = byKey.getOrElse(key, default)
  val name: StudyOrder => I18nKey =
    case StudyOrder.hot => I18nKey.study.hot
    case StudyOrder.newest => I18nKey.study.dateAddedNewest
    case StudyOrder.oldest => I18nKey.study.dateAddedOldest
    case StudyOrder.updated => I18nKey.study.recentlyUpdated
    case StudyOrder.popular => I18nKey.study.mostPopular
    case StudyOrder.alphabetical => I18nKey.study.alphabetical
    case StudyOrder.mine => I18nKey.study.myStudies
    case StudyOrder.relevant => I18nKey.study.relevant
