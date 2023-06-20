package lila.study

import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.i18n.{ I18nKey, I18nKeys as trans }
import lila.user.{ Me, User }

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using Executor):

  val maxPerPage                = lila.common.config.MaxPerPage(16)
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
      selectMemberId(me) ++ $doc("ownerId" $ne me.userId),
      order,
      page
    )

  def mineLikes(order: Order, page: Int)(using me: Me) =
    paginator(
      selectLiker(me) ++ accessSelect ++ $doc("ownerId" $ne me.userId),
      order,
      page
    )

  def byTopic(topic: StudyTopic, order: Order, page: Int)(using me: Option[Me]) =
    val onlyMine = me.ifTrue(order == Order.Mine)
    paginator(
      selectTopic(topic) ++ onlyMine.fold(accessSelect)(selectMemberId(_)),
      order,
      page,
      hint = onlyMine.isDefined option $doc("uids" -> 1, "rank" -> -1)
    )

  private def accessSelect(using me: Option[Me]) =
    me.fold(selectPublic): u =>
      $or(selectPublic, selectMemberId(u))

  private val noRelaySelect = $doc("from" $ne "relay")

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
        case Order.Hot          => $sort desc "rank"
        case Order.Newest       => $sort desc "createdAt"
        case Order.Oldest       => $sort asc "createdAt"
        case Order.Updated      => $sort desc "updatedAt"
        case Order.Popular      => $sort desc "likes"
        case Order.Alphabetical => $sort asc "name"
        // mine filter for topic view
        case Order.Mine => $sort desc "rank"
      ,
      hint = hint
    ) mapFutureList withChaptersAndLiking(me)
    Paginator(
      adapter = nbResults.fold(adapter): nb =>
        CachedAdapter(adapter, nb),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def withChaptersAndLiking(
      me: Option[User],
      nbChaptersPerStudy: Int = defaultNbChaptersPerStudy
  )(studies: Seq[Study]): Fu[Seq[Study.WithChaptersAndLiked]] =
    withChapters(studies, nbChaptersPerStudy) flatMap withLiking(me)

  private def withChapters(
      studies: Seq[Study],
      nbChaptersPerStudy: Int
  ): Fu[Seq[Study.WithChapters]] =
    chapterRepo.idNamesByStudyIds(studies.map(_.id), nbChaptersPerStudy) map { chapters =>
      studies.map { study =>
        Study.WithChapters(study, (chapters get study.id) so (_ map (_.name)))
      }
    }

  private def withLiking(
      me: Option[User]
  )(studies: Seq[Study.WithChapters]): Fu[Seq[Study.WithChaptersAndLiked]] =
    me.so { u =>
      studyRepo.filterLiked(u, studies.map(_.study.id))
    } map { liked =>
      studies.map { case Study.WithChapters(study, chapters) =>
        Study.WithChaptersAndLiked(study, chapters, liked(study.id))
      }
    }

enum Order(val key: String, val name: I18nKey):

  case Hot          extends Order("hot", trans.study.hot)
  case Newest       extends Order("newest", trans.study.dateAddedNewest)
  case Oldest       extends Order("oldest", trans.study.dateAddedOldest)
  case Updated      extends Order("updated", trans.study.recentlyUpdated)
  case Popular      extends Order("popular", trans.study.mostPopular)
  case Alphabetical extends Order("alphabetical", trans.study.alphabetical)
  case Mine         extends Order("mine", trans.study.myStudies)

object Order:
  val default                   = Hot
  val list                      = values.toList
  val withoutMine               = list.filterNot(_ == Mine)
  val withoutSelector           = withoutMine.filter(o => o != Oldest && o != Alphabetical)
  private val byKey             = list.mapBy(_.key)
  def apply(key: String): Order = byKey.getOrElse(key, default)
