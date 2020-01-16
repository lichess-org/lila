package lila.clas

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class ClasApi(
    teacherColl: Coll,
    clasColl: Coll,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def withTeacherOrCreate(user: User): Fu[Teacher.WithUser] =
    teacherColl.byId[Teacher](user.id) flatMap {
      case Some(teacher) => fuccess(Teacher.WithUser(teacher, user))
      case None =>
        val teacher = Teacher make user
        teacherColl.insert(teacher) inject Teacher.WithUser(teacher, user)
    }
}
