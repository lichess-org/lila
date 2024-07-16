package lila.coach

import play.api.i18n.Lang
import reactivemongo.api.*
import scalalib.paginator.{ AdapterLike, Paginator }

import lila.coach.CoachPager.Order.{ Alphabetical, LichessRating, Login }
import lila.core.perf.UserWithPerfs
import lila.core.perm.Permission
import lila.core.user.{ Flag, UserMark }
import lila.db.dsl.{ *, given }

final class CoachPager(
    userRepo: lila.core.user.UserRepo,
    perfsRepo: lila.core.user.PerfsRepo,
    coll: Coll
)(using Executor):

  val maxPerPage = MaxPerPage(10)

  import CoachPager.*
  import BsonHandlers.given

  def apply(
      lang: Option[Lang],
      order: Order,
      country: Option[Flag],
      page: Int
  ): Fu[Paginator[Coach.WithUser]] =
    def selector = listableSelector ++ lang.so { l => $doc("languages" -> l.code) }

    val adapter = new AdapterLike[Coach.WithUser]:
      def nbResults: Fu[Int] = coll.secondaryPreferred.countSel(selector)

      def slice(offset: Int, length: Int): Fu[List[Coach.WithUser]] =
        coll
          .aggregateList(length, _.sec): framework =>
            import framework.*
            Match(selector) -> List(
              Sort:
                order match
                  case Alphabetical  => Ascending("_id")
                  case LichessRating => Descending("user.rating")
                  case Login         => Descending("user.seenAt")
              ,
              PipelineOperator:
                $doc:
                  "$lookup" -> $doc(
                    "from"         -> userRepo.coll.name,
                    "localField"   -> "_id",
                    "foreignField" -> "_id",
                    "as"           -> "_user"
                  )
              ,
              UnwindField("_user"),
              Match(
                $doc(
                  s"_user.${lila.core.user.BSONFields.roles}"   -> Permission.Coach.dbKey,
                  s"_user.${lila.core.user.BSONFields.enabled}" -> true,
                  s"_user.${lila.core.user.BSONFields.marks}"
                    .$nin(List(UserMark.engine, UserMark.boost, UserMark.troll))
                ) ++ country.so { c =>
                  $doc("_user.profile.country" -> c.code)
                }
              ),
              Skip(offset),
              Limit(length),
              PipelineOperator(perfsRepo.aggregateLookup)
            )
          .map: docs =>
            import userRepo.userHandler
            for
              doc   <- docs
              coach <- doc.asOpt[Coach]
              user  <- doc.getAsOpt[User]("_user")
              perfs = perfsRepo.aggregateReadFirst(doc, user)
            yield coach.withUser(UserWithPerfs(user, perfs))

    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private val listableSelector = $doc(
    "listed"    -> Coach.Listed.Yes,
    "available" -> Coach.Available.Yes
  )

object CoachPager:

  enum Order(val key: String, val name: String):
    case Login         extends Order("login", "Last login")
    case LichessRating extends Order("rating", "Lichess rating")
    case Alphabetical  extends Order("alphabetical", "Alphabetical")

  object Order:
    val default                   = Login
    val list                      = values.toList
    def apply(key: String): Order = list.find(_.key == key) | default
