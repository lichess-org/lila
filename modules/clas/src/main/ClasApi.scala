package lila.clas

import org.joda.time.DateTime
import reactivemongo.api._

import lila.db.dsl._
import lila.user.{ User, UserRepo }
import lila.common.EmailAddress

final class ClasApi(
    colls: ClasColls,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  object teacher {

    val coll = colls.teacher

    def withOrCreate(user: User): Fu[Teacher.WithUser] =
      coll.byId[Teacher](user.id) flatMap {
        case Some(teacher) => fuccess(Teacher.WithUser(teacher, user))
        case None =>
          val teacher = Teacher make user
          coll.insert.one(teacher) inject Teacher.WithUser(teacher, user)
      }
  }

  object clas {

    val coll = colls.clas

    def of(teacher: Teacher): Fu[List[Clas]] =
      coll.ext
        .find($doc("teachers" -> teacher.id))
        .sort($sort desc "viewedAt")
        .list[Clas]()

    def create(data: ClasForm.Data, teacher: Teacher): Fu[Clas] = {
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas) inject clas
    }

    def update(from: Clas, data: ClasForm.Data): Fu[Clas] = {
      val clas = data update from
      coll.update.one($id(clas.id), clas) inject clas
    }

    def getAndView(id: Clas.Id, teacher: Teacher): Fu[Option[Clas]] =
      coll.ext
        .findAndUpdate[Clas](
          selector = $id(id) ++ $doc("teachers" -> teacher.id),
          update = $set("viewedAt"              -> DateTime.now),
          fetchNewObject = true
        )
  }

  object student {

    import lila.user.HashedPassword
    import User.ClearPassword

    val coll = colls.student

    def of(clas: Clas): Fu[List[Student.WithUser]] =
      coll.ext
        .find($doc("clasId" -> clas.id))
        .list[Student]() flatMap { students =>
        userRepo.coll.idsMap[User, User.ID](
          students.map(_.userId),
          ReadPreference.secondaryPreferred
        )(_.id) map { users =>
          students.flatMap { s =>
            users.get(s.userId) map { Student.WithUser(s, _) }
          }
        }
      }

    def get(clas: Clas, user: User): Fu[Option[Student.WithUser]] =
      coll.ext.one[Student]($id(Student.id(user.id, clas.id))) map2 {
        Student.WithUser(_, user)
      }

    def create(clas: Clas, username: String)(
        hashPassword: ClearPassword => HashedPassword
    ): Fu[(User, ClearPassword)] = {
      val email    = EmailAddress(s"noreply.class.${clas.id}.$username@lichess.org")
      val password = Student.password.generate
      userRepo
        .create(
          username = username,
          passwordHash = hashPassword(password),
          email = email,
          blind = false,
          mobileApiVersion = none,
          mustConfirmEmail = false
        )
        .orFail(s"No user could be created for $username")
        .flatMap { user =>
          coll.insert.one(Student.make(user, clas, managed = true)) inject
            (user -> password)
        }
    }
  }
}
