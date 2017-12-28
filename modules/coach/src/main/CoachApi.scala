package lila.coach

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.notify.{ Notification, NotifyApi }
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    reviewColl: Coll,
    photographer: Photographer,
    asyncCache: lila.memo.AsyncCache.Builder,
    notifyApi: NotifyApi
) {

  import BsonHandlers._

  private val cache = asyncCache.single[List[Coach]](
    name = "coach.list",
    f = coachColl.find($empty).list[Coach](),
    expireAfter = _.ExpireAfterWrite(30 minutes)
  )

  private def all = cache.get

  def byId(id: Coach.Id): Fu[Option[Coach]] = all.map(_.find(_.id == id))

  def find(username: String): Fu[Option[Coach.WithUser]] =
    UserRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Coach.WithUser]] = Granter(_.Coach)(user) ?? {
    byId(Coach.Id(user.id)) map2 withUser(user)
  }

  def findOrInit(user: User): Fu[Option[Coach.WithUser]] = Granter(_.Coach)(user) ?? {
    find(user) orElse {
      val c = Coach.WithUser(Coach make user, user)
      coachColl.insert(c.coach) >>- cache.refresh inject c.some
    }
  }

  def isListedCoach(user: User): Fu[Boolean] =
    Granter(_.Coach)(user) ?? all.map(_.exists { c =>
      c.is(user) && c.isListed
    })

  def listedWithUserList: Fu[List[Coach.WithUser]] =
    all.map(_.filter(_.isListed)) flatMap { coaches =>
      UserRepo.byIdsSecondary(coaches.map(_.id.value)) map { users =>
        coaches.flatMap { coach =>
          users find coach.is filter Granter(_.Coach) map { Coach.WithUser(coach, _) }
        }
      }
    }

  def update(c: Coach.WithUser, data: CoachProfileForm.Data): Funit =
    coachColl.update(
      $id(c.coach.id),
      data(c.coach),
      upsert = true
    ).void >>- cache.refresh

  def setNbReviews(id: Coach.Id, nb: Int): Funit =
    coachColl.update($id(id), $set("nbReviews" -> nb)).void >>- cache.refresh

  private[coach] def toggleApproved(username: String, value: Boolean): Fu[String] =
    find(username) flatMap {
      case None => fuccess("No such coach")
      case Some(c) => coachColl.update(
        $id(c.coach.id),
        $set("approved" -> value)
      ) >>- cache.refresh inject "Done!"
    }

  def uploadPicture(c: Coach.WithUser, picture: Photographer.Uploaded): Funit =
    photographer(c.coach.id.value, picture).flatMap { pic =>
      coachColl.update($id(c.coach.id), $set("picturePath" -> pic.path)).void
    } >>- cache.refresh

  def deletePicture(c: Coach.WithUser): Funit =
    coachColl.update($id(c.coach.id), $unset("picturePath")).void >>- cache.refresh

  private def withUser(user: User)(coach: Coach): Coach.WithUser =
    Coach.WithUser(coach, user)

  object reviews {

    def add(me: User, coach: Coach, data: CoachReviewForm.Data): Fu[CoachReview] =
      find(me, coach).flatMap { existing =>
        val id = CoachReview.makeId(me, coach)
        val review = existing match {
          case None => CoachReview(
            _id = id,
            userId = me.id,
            coachId = coach.id,
            score = data.score,
            text = data.text,
            approved = false,
            createdAt = DateTime.now,
            updatedAt = DateTime.now
          )
          case Some(r) => r.copy(
            score = data.score,
            text = data.text,
            approved = false,
            updatedAt = DateTime.now
          )
        }
        if (me.troll) fuccess(review)
        else {
          reviewColl.update($id(id), review, upsert = true) >>
            notifyApi.addNotification(Notification.make(
              notifies = Notification.Notifies(coach.id.value),
              content = lila.notify.CoachReview
            )) >> refreshCoachNbReviews(coach.id) inject review
        }
      }

    def byId(id: String) = reviewColl.byId[CoachReview](id)

    def mine(user: User, coach: Coach): Fu[Option[CoachReview]] =
      reviewColl.byId[CoachReview](CoachReview.makeId(user, coach))

    def approve(r: CoachReview, v: Boolean) = {
      if (v) reviewColl.update(
        $id(r.id),
        $set("approved" -> v) ++ $unset("moddedAt")
      ).void
      else reviewColl.remove($id(r.id)).void
    } >> refreshCoachNbReviews(r.coachId)

    def mod(r: CoachReview) = reviewColl.update($id(r.id), $set(
      "approved" -> false,
      "moddedAt" -> DateTime.now
    )) >> refreshCoachNbReviews(r.coachId)

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

    def deleteAllBy(userId: User.ID): Funit = for {
      reviews <- reviewColl.find($doc("userId" -> userId)).list[CoachReview]
      _ <- reviews.map { review =>
        reviewColl.remove($doc("userId" -> review.userId)).void
      }.sequenceFu
      _ <- reviews.map(_.coachId).distinct.map(refreshCoachNbReviews).sequenceFu
    } yield ()

    private def findRecent(selector: Bdoc): Fu[CoachReview.Reviews] =
      reviewColl.find(selector)
        .sort($sort desc "createdAt")
        .list[CoachReview](100) map CoachReview.Reviews.apply
  }
}
