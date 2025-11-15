package lila.clas

import reactivemongo.api.*
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.id.ClasId
import lila.db.dsl.{ *, given }

case class ClasLogin(@Key("_id") id: ClasId, created: Clas.Recorded, codes: List[ClasUserCode]):
  def expiresAt: Instant = created.at.plusMinutes(15)

case class ClasUserCode(user: UserId, code: String)

final class ClasLoginApi(colls: ClasColls, userRepo: lila.user.UserRepo)(using Executor):
  private val coll = colls.login
  import BsonHandlers.given

  def create(clas: Clas, students: List[Student])(using teacher: Me): Fu[ClasLogin] = for
    cur <- coll.exists($id(clas.id))
    _ <- cur.so(coll.delete.one($id(clas.id)).void)
    login = ClasLogin(clas.id, Clas.Recorded(teacher.userId, nowInstant), makeCodes(students))
    _ <- coll.insert.one(login)
  yield login

  def get(clasId: ClasId) = coll.byId[ClasLogin](clasId)

  def login(code: String): Fu[Option[(User, ClasId)]] =
    coll
      .find($doc("codes.code" -> code))
      .one[ClasLogin]
      .flatMapz: login =>
        login.codes
          .find(_.code == code)
          .map(_.user)
          .so(userRepo.enabledById)
          .map2(_ -> login.id)

  private def makeCodes(students: List[Student]): List[ClasUserCode] =
    val codes = students.collect:
      case s if s.managed && s.isActive => ClasUserCode(s.userId, Student.password.generate(5).value)

    // detect duplicates (very unlikely)
    if codes.map(_.code).distinct.size != codes.size
    then makeCodes(students)
    else codes
