package lila.clas

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import reactivemongo.core.errors.DatabaseException

final class ClasMatesCache(colls: ClasColls, cacheApi: CacheApi, studentCache: ClasStudentCache)(using
    Executor
):

  def get(studentId: UserId): Fu[Set[UserId]] =
    studentCache.isStudent(studentId) so cache.get(studentId)

  private val cache = cacheApi[UserId, Set[UserId]](256, "clas.mates") {
    _.expireAfterWrite(5 minutes)
      .buildAsyncFuture(fetchMatesAndTeachers)
  }

  private def fetchMatesAndTeachers(studentId: UserId): Fu[Set[UserId]] =
    colls.student
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc("userId" -> studentId)) -> List(
          Group(BSONNull)("classes" -> PushField("clasId")),
          Facet(
            List(
              "mates" -> List(
                PipelineOperator(
                  $lookup.pipelineFull(
                    from = colls.student.name,
                    as = "mates",
                    let = $doc("ids" -> "$classes"),
                    pipe = List(
                      $doc(
                        "$match" -> $expr(
                          $and(
                            $doc("$in" -> $arr("$clasId", "$$ids")),
                            $doc("$ne" -> $arr("$userId", studentId))
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
                ),
                ReplaceRoot($doc("$arrayElemAt" -> $arr("$mates", 0)))
              ),
              "teachers" -> List(
                PipelineOperator(
                  $lookup.pipelineFull(
                    from = colls.clas.name,
                    as = "teachers",
                    let = $doc("ids" -> "$classes"),
                    pipe = List(
                      $doc("$match"  -> $expr($doc("$in" -> $arr("$_id", "$$ids")))),
                      $doc("$unwind" -> "$teachers"),
                      $doc(
                        "$group" -> $doc(
                          "_id"      -> BSONNull,
                          "teachers" -> $doc("$addToSet" -> "$teachers")
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
      .map: docO =>
        for
          doc      <- docO
          mates    <- doc.getAsOpt[Set[UserId]]("mates")
          teachers <- doc.getAsOpt[Set[UserId]]("teachers")
        yield mates ++ teachers
      .dmap(~_)
      .recover:
        // can happen, probably in case of student cache bloom filter false positive
        case e: DatabaseException if e.getMessage.contains("resulting value was: MISSING") => Set.empty
