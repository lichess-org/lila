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

  def init(username: String): Fu[String] = find(username) flatMap {
    case Some(_) => fuccess(s"Coach $username already exists.")
    case None => UserRepo named username flatMap {
      case None       => fuccess(s"No such username $username")
      case Some(user) => coll.insert(Coach make user) inject "Done!"
    }
  }

  private def withUser(user: User)(coach: Coach): Coach.WithUser =
    Coach.WithUser(coach, user)
}
