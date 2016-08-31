package lila.coach

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.AsyncCache
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    reviewColl: Coll,
    photographer: Photographer) {

  import BsonHandlers._

  private val cache = AsyncCache.single[List[Coach]](
    f = coachColl.find($empty).list[Coach](),
    timeToLive = 1 hour)

  private def all = cache(true)

  def byId(id: Coach.Id): Fu[Option[Coach]] = all.map(_.find(_.id == id))

  def find(username: String): Fu[Option[Coach.WithUser]] =
    UserRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Coach.WithUser]] =
    byId(Coach.Id(user.id)) map2 withUser(user)

  def findOrInit(user: User): Fu[Option[Coach.WithUser]] = find(user) orElse {
    fuccess(Coach.WithUser(Coach make user, user).some)
  }

  def isEnabledCoach(user: User): Fu[Boolean] =
    Granter(_.Coach)(user) ?? all.map(_.exists { c =>
      c.is(user) && c.isFullyEnabled
    })

  def enabledWithUserList: Fu[List[Coach.WithUser]] =
    all.map(_.filter(_.isFullyEnabled)) flatMap { coaches =>
      UserRepo.byIds(coaches.map(_.id.value)) map { users =>
        coaches.flatMap { coach =>
          users find coach.is map { Coach.WithUser(coach, _) }
        }
      }
    }

  def update(c: Coach.WithUser, data: CoachProfileForm.Data): Funit =
    coachColl.update(
      $id(c.coach.id),
      data(c.coach),
      upsert = true
    ).void >> cache.clear

  private[coach] def toggleByMod(username: String, value: Boolean): Fu[String] =
    find(username) flatMap {
      case None => fuccess("No such coach")
      case Some(c) => coachColl.update(
        $id(c.coach.id),
        $set("enabledByMod" -> value)
      ) >> cache.clear inject "Done!"
    }

  def uploadPicture(c: Coach.WithUser, picture: Photographer.Uploaded): Funit =
    photographer(c.coach.id, picture).flatMap { pic =>
      coachColl.update($id(c.coach.id), $set("picturePath" -> pic.path))
    } >> cache.clear

  def deletePicture(c: Coach.WithUser): Funit =
    coachColl.update($id(c.coach.id), $unset("picturePath")) >> cache.clear

  private def withUser(user: User)(coach: Coach): Coach.WithUser =
    Coach.WithUser(coach, user)
}
