package lila.coach

import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi
import lila.notify.NotifyApi
import lila.security.Granter
import lila.user.{ Holder, User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    reviewColl: Coll,
    userRepo: UserRepo,
    picfitApi: PicfitApi,
    cacheApi: lila.memo.CacheApi,
    notifyApi: NotifyApi
)(using Executor):

  import BsonHandlers.given

  def byId[U](u: U)(using idOf: UserIdOf[U]): Fu[Option[Coach]] = coachColl.byId[Coach](idOf(u))

  def find(username: UserStr): Fu[Option[Coach.WithUser]] =
    userRepo byId username flatMapz find

  def find(user: User): Fu[Option[Coach.WithUser]] =
    Granter(_.Coach)(user) ?? {
      byId(user.id) dmap {
        _ map withUser(user)
      }
    }

  def findOrInit(coach: Holder): Fu[Option[Coach.WithUser]] =
    Granter.is(_.Coach)(coach) ?? {
      find(coach.user) orElse {
        val c = Coach.WithUser(Coach make coach.user, coach.user)
        coachColl.insert.one(c.coach) inject c.some
      }
    }

  def isListedCoach(user: User): Fu[Boolean] =
    Granter(_.Coach)(user) ?? user.enabled.yes ?? user.marks.clean ?? coachColl.exists(
      $id(user.id) ++ $doc("listed" -> true)
    )

  def setSeenAt(user: User): Funit =
    Granter(_.Coach)(user) ?? coachColl.update.one($id(user.id), $set("user.seenAt" -> nowInstant)).void

  def setRating(userPre: User): Funit =
    Granter(_.Coach)(userPre) ?? {
      userRepo.byId(userPre.id) flatMapz { user =>
        coachColl.update.one($id(user.id), $set("user.rating" -> user.perfs.bestStandardRating)).void
      }
    }

  def update(c: Coach.WithUser, data: CoachProfileForm.Data): Funit =
    coachColl.update
      .one(
        $id(c.coach.id),
        data(c.coach),
        upsert = true
      )
      .void

  def setNbReviews(id: Coach.Id, nb: Int): Funit =
    coachColl.update.one($id(id), $set("nbReviews" -> nb)).void

  def uploadPicture(c: Coach.WithUser, picture: PicfitApi.FilePart): Funit =
    picfitApi
      .uploadFile(s"coach:${c.coach.id}", picture, userId = c.user.id) flatMap { pic =>
      coachColl.update.one($id(c.coach.id), $set("picture" -> pic.id)).void
    }

  private val languagesCache = cacheApi.unit[Set[String]] {
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture { _ =>
        coachColl.secondaryPreferred.distinctEasy[String, Set]("languages", $empty)
      }
  }
  def allLanguages: Fu[Set[String]] = languagesCache.get {}

  private val countriesCache = cacheApi.unit[Set[String]] {
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture { _ =>
        userRepo.coll.secondaryPreferred
          .distinctEasy[String, Set](
            "profile.country",
            $doc("roles" -> lila.security.Permission.Coach.dbKey, "enabled" -> true)
          )
      }
  }
  def allCountries: Fu[Set[String]] = countriesCache.get {}

  private def withUser(user: User)(coach: Coach) = Coach.WithUser(coach, user)

  object reviews:

    def add(me: User, coach: Coach, data: CoachReviewForm.Data): Fu[CoachReview] =
      find(me, coach).flatMap { existing =>
        val id = CoachReview.makeId(me, coach)
        val review = existing match
          case None =>
            CoachReview(
              _id = id,
              userId = me.id,
              coachId = coach.id,
              score = data.score,
              text = data.text,
              approved = false,
              createdAt = nowInstant,
              updatedAt = nowInstant
            )
          case Some(r) =>
            r.copy(
              score = data.score,
              text = data.text,
              approved = false,
              updatedAt = nowInstant
            )
        if (me.marks.troll) fuccess(review)
        else
          reviewColl.update.one($id(id), review, upsert = true) >>
            notifyApi.notifyOne(coach.id, lila.notify.CoachReview) >>
            refreshCoachNbReviews(coach.id) inject review
      }

    def byId(id: String) = reviewColl.byId[CoachReview](id)

    def approve(r: CoachReview, v: Boolean) = {
      if (v)
        reviewColl.update
          .one(
            $id(r.id),
            $set("approved" -> v) ++ $unset("moddedAt")
          )
          .void
      else reviewColl.delete.one($id(r.id)).void
    } >> refreshCoachNbReviews(r.coachId)

    def mod(r: CoachReview) =
      reviewColl.update.one(
        $id(r.id),
        $set(
          "approved" -> false,
          "moddedAt" -> nowInstant
        )
      ) >> refreshCoachNbReviews(r.coachId)

    private def refreshCoachNbReviews(id: Coach.Id): Funit =
      reviewColl.countSel($doc("coachId" -> id.value, "approved" -> true)) flatMap {
        setNbReviews(id, _)
      }

    def find(user: User, coach: Coach): Fu[Option[CoachReview]] =
      reviewColl.byId[CoachReview](CoachReview.makeId(user, coach))

    def approvedByCoach(c: Coach): Fu[CoachReview.Reviews] =
      findRecent($doc("coachId" -> c.id.value, "approved" -> true))

    def pendingByCoach(c: Coach): Fu[CoachReview.Reviews] =
      findRecent($doc("coachId" -> c.id.value, "approved" -> false))

    def allByCoach(c: Coach): Fu[CoachReview.Reviews] =
      findRecent($doc("coachId" -> c.id.value))

    def allByPoster(user: User): Fu[CoachReview.Reviews] =
      findRecent($doc("userId" -> user.id))

    def deleteAllBy(userId: UserId): Funit =
      for {
        reviews <- reviewColl.list[CoachReview]($doc("userId" -> userId))
        _ <- reviews.map { review =>
          reviewColl.delete.one($doc("userId" -> review.userId)).void
        }.parallel
        _ <- reviews.map(_.coachId).distinct.map(refreshCoachNbReviews).parallel
      } yield ()

    private def findRecent(selector: Bdoc): Fu[CoachReview.Reviews] =
      reviewColl
        .find(selector)
        .sort($sort desc "createdAt")
        .cursor[CoachReview]()
        .list(100) map CoachReview.Reviews.apply
