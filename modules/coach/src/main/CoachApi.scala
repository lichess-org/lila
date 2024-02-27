package lila.coach

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    userRepo: UserRepo,
    photographer: Photographer,
    cacheApi: lila.memo.CacheApi
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

  def findOrInit(user: User): Fu[Option[Coach.WithUser]] =
    Granter(_.Coach)(user) ?? {
      find(user) orElse {
        val c = Coach.WithUser(Coach make user, user)
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

  private def withUser(user: User)(coach: Coach) = Coach.WithUser(coach, user)

}
