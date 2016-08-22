package lila.coach

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class CoachApi(coll: Coll) {

  import BsonHandlers._

  def find(username: String): Fu[Option[Coach.WithUser]] =
    UserRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Coach.WithUser]] =
    coll.byId[Coach](user.id) map2 withUser(user)

  def enabledWithUserList: Fu[List[Coach.WithUser]] =
    coll.list[Coach]($doc("enabled" -> true)) flatMap { coaches =>
      UserRepo.byIds(coaches.map(_.id.value)) map { users =>
        coaches.flatMap { coach =>
          users find coach.is map { Coach.WithUser(coach, _) }
        }
      }
    }

  def update(c: Coach.WithUser, data: CoachForm.Data): Funit =
    coll.update($id(c.coach.id), data(c.coach)).void

  private def withUser(user: User)(coach: Coach): Coach.WithUser =
    Coach.WithUser(coach, user)
}
