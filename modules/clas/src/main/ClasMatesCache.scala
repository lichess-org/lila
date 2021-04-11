package lila.clas

import play.api.Mode
import reactivemongo.api.bson.BSONNull
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User
import reactivemongo.core.errors.DatabaseException

final class ClasMatesCache(colls: ClasColls, cacheApi: CacheApi, studentCache: ClasStudentCache)(implicit
    ec: ExecutionContext,
    mode: Mode
) {

  def get(studentId: User.ID): Fu[Set[User.ID]] =
    studentCache.isStudent(studentId) ?? cache.get(studentId)

  private val cache = cacheApi[User.ID, Set[User.ID]](256, "clas.mates") {
    _.expireAfterWrite(5 minutes)
      .buildAsyncFuture(fetchMatesAndTeachers)
  }

  private def fetchMatesAndTeachers(studentId: User.ID): Fu[Set[User.ID]] =
    colls.student
      .aggregateOne(ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match($doc("userId" -> studentId)) -> List(
          Group(BSONNull)("classes" -> PushField("clasId")),
          Facet(
            List(
              "mates" -> List(
                PipelineOperator(
                  $doc(
                    "$lookup" -> $doc(
                      "from" -> colls.student.name,
                      "as"   -> "mates",
                      "let"  -> $doc("ids" -> "$classes"),
                      "pipeline" -> $arr(
                        $doc(
                          "$match" -> $doc(
                            "$expr" -> $doc(
                              "$and" -> $arr(
                                $doc("$in" -> $arr("$clasId", "$$ids")),
                                $doc("$ne" -> $arr("$userId", studentId))
                              )
                            )
                          )
                        ),
                        $doc(
                          "$group" -> $doc(
                            "_id"   -> BSONNull,
                            "mates" -> $doc("$addToSet" -> "$userId")
                          )
                        )
                      )
                    )
                  )
                ),
                ReplaceRoot($doc("$arrayElemAt" -> $arr("$mates", 0)))
              ),
              "teachers" -> List(
                PipelineOperator(
                  $doc(
                    "$lookup" -> $doc(
                      "from" -> colls.clas.name,
                      "as"   -> "teachers",
                      "let"  -> $doc("ids" -> "$classes"),
                      "pipeline" -> $arr(
                        $doc(
                          "$match" -> $doc(
                            "$expr" -> $doc("$in" -> $arr("$_id", "$$ids"))
                          )
                        ),
                        $doc("$unwind" -> "$teachers"),
                        $doc(
                          "$group" -> $doc(
                            "_id"      -> BSONNull,
                            "teachers" -> $doc("$addToSet" -> "$teachers")
                          )
                        )
                      )
                    )
                  )
                ),
                ReplaceRoot($doc("$arrayElemAt" -> $arr("$teachers", 0)))
              )
            )
          ),
          ReplaceRoot(
            $doc(
              "$mergeObjects" -> $arr(
                $doc("$arrayElemAt" -> $arr("$mates", 0)),
                $doc("$arrayElemAt" -> $arr("$teachers", 0))
              )
            )
          )
        )
      }
      .map { docO =>
        for {
          doc      <- docO
          mates    <- doc.getAsOpt[Set[User.ID]]("mates")
          teachers <- doc.getAsOpt[Set[User.ID]]("teachers")
        } yield mates ++ teachers
      }
      .dmap(~_)
      .recover {
        // can happen, probably in case of student cache bloom filter false positive
        case e: DatabaseException if e.getMessage.contains("resulting value was: MISSING") => Set.empty
      }
}
