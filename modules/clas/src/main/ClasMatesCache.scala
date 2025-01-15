package lila.clas

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

final class ClasMatesCache(colls: ClasColls, cacheApi: CacheApi, studentCache: ClasStudentCache)(using
    Executor
):

  def get(studentId: UserId): Fu[Set[UserId]] =
    studentCache.isStudent(studentId).so(cache.get(studentId))

  private val cache = cacheApi[UserId, Set[UserId]](64, "clas.mates"):
    _.expireAfterWrite(5.minutes)
      .buildAsyncFuture(fetchMatesAndTeachers)

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
                ReplaceRoot:
                  $ifNull(
                    $doc("$arrayElemAt" -> $arr("$mates", 0)),
                    $doc("mates"        -> $arr())
                  )
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
                ReplaceRoot:
                  $ifNull(
                    $doc("$arrayElemAt" -> $arr("$teachers", 0)),
                    $doc("teachers"     -> $arr())
                  )
              )
            )
          ),
          ReplaceRoot:
            $doc(
              "$mergeObjects" -> $arr(
                $doc("$arrayElemAt" -> $arr("$mates", 0)),
                $doc("$arrayElemAt" -> $arr("$teachers", 0))
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
