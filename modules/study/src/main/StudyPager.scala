package lila.study

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.i18n.{ I18nKey, I18nKeys => trans }
import lila.user.User

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  val maxPerPage                = lila.common.config.MaxPerPage(16)
  val defaultNbChaptersPerStudy = 4

  import BSONHandlers._
  import studyRepo.{
    selectLiker,
    selectMemberId,
    selectOwnerId,
    selectPrivateOrUnlisted,
    selectPublic,
    selectTopic
  }

  def all(me: Option[User], order: Order, page: Int) =
    paginator(
      accessSelect(me),
      me,
      order,
      page,
      fuccess(9999).some
    )

  def byOwner(owner: User, me: Option[User], order: Order, page: Int) =
    paginator(
      selectOwnerId(owner.id) ++ accessSelect(me),
      me,
      order,
      page
    )

  def mine(me: User, order: Order, page: Int) =
    paginator(
      selectOwnerId(me.id),
      me.some,
      order,
      page
    )

  def minePublic(me: User, order: Order, page: Int) =
    paginator(
      selectOwnerId(me.id) ++ selectPublic,
      me.some,
      order,
      page
    )

  def minePrivate(me: User, order: Order, page: Int) =
    paginator(
      selectOwnerId(me.id) ++ selectPrivateOrUnlisted,
      me.some,
      order,
      page
    )

  def mineMember(me: User, order: Order, page: Int) =
    paginator(
      selectMemberId(me.id) ++ $doc("ownerId" $ne me.id),
      me.some,
      order,
      page
    )

  def mineLikes(me: User, order: Order, page: Int) =
    paginator(
      selectLiker(me.id) ++ accessSelect(me.some) ++ $doc("ownerId" $ne me.id),
      me.some,
      order,
      page
    )

  def byTopic(topic: StudyTopic, me: Option[User], order: Order, page: Int) = {
    val onlyMine = me.ifTrue(order == Order.Mine)
    paginator(
      selectTopic(topic) ++ onlyMine.fold(accessSelect(me))(m => selectMemberId(m.id)),
      me,
      order,
      page,
      hint = onlyMine.isDefined option $doc("uids" -> 1, "rank" -> -1)
    )
  }

  private def accessSelect(me: Option[User]) =
    me.fold(selectPublic) { u =>
      $or(selectPublic, selectMemberId(u.id))
    }

  private def paginator(
      selector: Bdoc,
      me: Option[User],
      order: Order,
      page: Int,
      nbResults: Option[Fu[Int]] = none,
      hint: Option[Bdoc] = none
  ): Fu[Paginator[Study.WithChaptersAndLiked]] = studyRepo.coll { coll =>
    val adapter = new Adapter[Study](
      collection = coll,
      selector = selector,
      projection = studyRepo.projection.some,
      sort = order match {
        case Order.Hot          => $sort desc "rank"
        case Order.Newest       => $sort desc "createdAt"
        case Order.Oldest       => $sort asc "createdAt"
        case Order.Updated      => $sort desc "updatedAt"
        case Order.Popular      => $sort desc "likes"
        case Order.Alphabetical => $sort asc "name"
        // mine filter for topic view
        case Order.Mine => $sort desc "rank"
      },
      hint = hint
    ) mapFutureList withChaptersAndLiking(me)
    Paginator(
      adapter = nbResults.fold(adapter) { nb =>
        new CachedAdapter(adapter, nb)
      },
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

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
        Study.WithChapters(study, (chapters get study.id) ?? (_ map (_.name)))
      }
    }

  private def withLiking(
      me: Option[User]
  )(studies: Seq[Study.WithChapters]): Fu[Seq[Study.WithChaptersAndLiked]] =
    me.?? { u =>
      studyRepo.filterLiked(u, studies.map(_.study.id))
    } map { liked =>
      studies.map { case Study.WithChapters(study, chapters) =>
        Study.WithChaptersAndLiked(study, chapters, liked(study.id))
      }
    }
}

sealed abstract class Order(val key: String, val name: I18nKey)

object Order {
  case object Hot          extends Order("hot", trans.study.hot)
  case object Newest       extends Order("newest", trans.study.dateAddedNewest)
  case object Oldest       extends Order("oldest", trans.study.dateAddedOldest)
  case object Updated      extends Order("updated", trans.study.recentlyUpdated)
  case object Popular      extends Order("popular", trans.study.mostPopular)
  case object Alphabetical extends Order("alphabetical", trans.study.alphabetical)
  case object Mine         extends Order("mine", trans.study.myStudies)

  val default         = Hot
  val all             = List(Hot, Newest, Oldest, Updated, Popular, Alphabetical)
  val withoutSelector = all.filter(o => o != Oldest && o != Alphabetical)
  val allWithMine     = Mine :: all
  private val byKey: Map[String, Order] = allWithMine.map { o =>
    o.key -> o
  }.toMap
  def apply(key: String): Order = byKey.getOrElse(key, default)
}
