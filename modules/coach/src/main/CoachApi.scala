package lila.coach

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.AsyncCache
import lila.user.{ User, UserRepo }

final class CoachApi(
    coll: Coll,
    photographer: Photographer) {

  import BsonHandlers._

  private val cache = AsyncCache.single[List[Coach]](
    f = coll.find($empty).list[Coach](),
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

  def enabledWithUserList: Fu[List[Coach.WithUser]] =
    all.map(_.filter(_.fullyEnabled)) flatMap { coaches =>
      UserRepo.byIds(coaches.map(_.id.value)) map { users =>
        coaches.flatMap { coach =>
          users find coach.is map { Coach.WithUser(coach, _) }
        }
      }
    }

  def update(c: Coach.WithUser, data: CoachForm.Data): Funit =
    coll.update($id(c.coach.id), data(c.coach)).void >> cache.clear

  private[coach] def toggleByMod(username: String, value: Boolean): Fu[String] =
    find(username) flatMap {
      case None => fuccess("No such coach")
      case Some(c) => coll.update(
        $id(c.coach.id),
        $set("enabledByMod" -> value)
      ) >> cache.clear inject "Done!"
    }

  private val pictureMaxMb = 3
  private val pictureMaxBytes = pictureMaxMb * 1024 * 1024
  private def pictureId(id: Coach.Id) = s"coach:${id.value}:picture"

  def uploadPicture(c: Coach.WithUser, picture: Photographer.Uploaded): Funit =
    photographer(c.coach.id, picture) flatMap { pic =>
      coll.update($id(c.coach.id), $set("picturePath" -> pic.path))
    } >> cache.clear

  def deletePicture(c: Coach.WithUser): Funit =
    coll.update($id(c.coach.id), $unset("picturePath")) >> cache.clear

  private def withUser(user: User)(coach: Coach): Coach.WithUser =
    Coach.WithUser(coach, user)
}
