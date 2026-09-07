package lila.clas

import lila.db.dsl.{ *, given }
import lila.core.config.CollName
import lila.core.id.ClasId

private final class ClasColls(db: lila.db.Db):
  val clas = db(CollName("clas_clas"))
  val student = db(CollName("clas_student"))
  val invite = db(CollName("clas_invite"))
  val login = db(CollName("clas_login"))

  // works for clas & student
  def selectArchived(v: Boolean) = $doc("archived".$exists(v))

  def clasIdsOfStudent(userId: UserId)(using Executor): Fu[List[ClasId]] =
    student.distinctEasy[ClasId, List]("clasId", $doc("userId" -> userId) ++ selectArchived(false), _.sec)
