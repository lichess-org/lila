package lila.clas

import org.joda.time.DateTime

import lila.db.dsl._
import lila.user.User

final class ClasApi(
    teacherColl: Coll,
    clasColl: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  object teacher {

    val coll = teacherColl

    def withOrCreate(user: User): Fu[Teacher.WithUser] =
      coll.byId[Teacher](user.id) flatMap {
        case Some(teacher) => fuccess(Teacher.WithUser(teacher, user))
        case None =>
          val teacher = Teacher make user
          coll.insert.one(teacher) inject Teacher.WithUser(teacher, user)
      }
  }

  object clas {

    val coll = clasColl

    def of(teacher: Teacher): Fu[List[Clas]] =
      coll.ext
        .find($doc("ownerId" -> teacher.id))
        .sort($sort desc "viewedAt")
        .list[Clas]()

    def create(data: ClasForm.Data, teacher: Teacher): Fu[Clas] = {
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas) inject clas
    }

    def getAndView(id: Clas.Id, teacher: Teacher): Fu[Option[Clas]] =
      coll.ext
        .findAndUpdate[Clas](
          selector = $id(id) ++ $doc("teachers" -> teacher.id),
          update = $set("viewedAt"              -> DateTime.now),
          fetchNewObject = true
        )
  }
}
