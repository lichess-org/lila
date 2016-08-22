package lila.coach

import org.joda.time.DateTime

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class CoachApi(
    coll: Coll,
    imageColl: Coll) {

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

  private val pictureMaxMb = 3
  private val pictureMaxBytes = pictureMaxMb * 1024 * 1024
  private def pictureId(id: Coach.Id) = s"coach:${id.value}:picture"

  def uploadPicture(
    c: Coach.WithUser,
    picture: play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]): Funit =
    if (picture.ref.file.length > pictureMaxBytes) fufail(s"File size must not exceed ${pictureMaxMb}MB.")
    else {
      val image = lila.db.DbImage.make(
        id = pictureId(c.coach.id),
        name = picture.filename,
        contentType = picture.contentType,
        file = picture.ref.file)
      imageColl.update($id(image.id), image, upsert = true) >>
        coll.update($id(c.coach.id), $set("picturePath" -> image.path))
    } void

  def deletePicture(c: Coach.WithUser): Funit =
    coll.update($id(c.coach.id), $unset("picturePath")).void

  private def withUser(user: User)(coach: Coach): Coach.WithUser =
    Coach.WithUser(coach, user)
}
