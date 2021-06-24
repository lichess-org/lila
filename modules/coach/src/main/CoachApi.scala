package lila.coach

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.notify.{ Notification, NotifyApi }
import lila.security.Granter
import lila.user.{ Holder, User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    reviewColl: Coll,
    userRepo: UserRepo,
    photographer: Photographer,
    cacheApi: lila.memo.CacheApi,
    notifyApi: NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def byId(id: Coach.Id): Fu[Option[Coach]] = coachColl.byId[Coach](id.value)

  def find(username: String): Fu[Option[Coach.WithUser]] =
    userRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Coach.WithUser]] =
    Granter(_.Coach)(user) ?? {
      byId(Coach.Id(user.id)) dmap {
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
    Granter(_.Coach)(user) ?? coachColl.exists($id(user.id) ++ $doc("listed" -> true))

  def setSeenAt(user: User): Funit =
    Granter(_.Coach)(user) ?? coachColl.update.one($id(user.id), $set("user.seenAt" -> DateTime.now)).void

  def setRating(userPre: User): Funit =
    Granter(_.Coach)(userPre) ?? {
      userRepo.byId(userPre.id) flatMap {
        _ ?? { user =>
          coachColl.update.one($id(user.id), $set("user.rating" -> user.perfs.bestStandardRating)).void
        }
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

  private[coach] def toggleApproved(username: String, value: Boolean): Fu[String] =
    coachColl.update.one(
      $id(User.normalize(username)),
      $set("approved" -> value)
    ) dmap { result =>
      if (result.n > 0) "Done!"
      else "No such coach"
    }

  def remove(userId: User.ID): Funit = coachColl.updateField($id(userId), "listed", false).void

  def uploadPicture(c: Coach.WithUser, picture: Photographer.Uploaded, by: User): Funit =
    photographer(c.coach.id.value, picture, createdBy = by.id).flatMap { pic =>
      coachColl.update.one($id(c.coach.id), $set("picturePath" -> pic.path)).void
    }

  def deletePicture(c: Coach.WithUser): Funit =
    coachColl.update.one($id(c.coach.id), $unset("picturePath")).void

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

  object reviews {

    def add(me: User, coach: Coach, data: CoachReviewForm.Data): Fu[CoachReview] =
      find(me, coach).flatMap { existing =>
        val id = CoachReview.makeId(me, coach)
        val review = existing match {
          case None =>
            CoachReview(
              _id = id,
              userId = me.id,
              coachId = coach.id,
              score = data.score,
              text = data.text,
              approved = false,
              createdAt = DateTime.now,
              updatedAt = DateTime.now
            )
          case Some(r) =>
            r.copy(
              score = data.score,
              text = data.text,
              approved = false,
              updatedAt = DateTime.now
            )
        }
        if (me.marks.troll) fuccess(review)
        else {
          reviewColl.update.one($id(id), review, upsert = true) >>
            notifyApi.addNotification(
              Notification.make(
                notifies = Notification.Notifies(coach.id.value),
                content = lila.notify.CoachReview
              )
            ) >> refreshCoachNbReviews(coach.id) inject review
        }
      }

    def byId(id: String) = reviewColl.byId[CoachReview](id)

    def mine(user: User, coach: Coach): Fu[Option[CoachReview]] =
      reviewColl.byId[CoachReview](CoachReview.makeId(user, coach))

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
          "moddedAt" -> DateTime.now
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

    def deleteAllBy(userId: User.ID): Funit =
      for {
        reviews <- reviewColl.list[CoachReview]($doc("userId" -> userId))
        _ <- reviews.map { review =>
          reviewColl.delete.one($doc("userId" -> review.userId)).void
        }.sequenceFu
        _ <- reviews.map(_.coachId).distinct.map(refreshCoachNbReviews).sequenceFu
      } yield ()

    private def findRecent(selector: Bdoc): Fu[CoachReview.Reviews] =
      reviewColl
        .find(selector)
        .sort($sort desc "createdAt")
        .cursor[CoachReview]()
        .list(100) map CoachReview.Reviews.apply
  }
}
