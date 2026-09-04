package lila.clas

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.clas.BsonHandlers.given

final class ClasMates(colls: ClasColls, cacheApi: CacheApi, filters: ClasUserFilters)(using
    Executor
):

  def get(studentId: UserId): Fu[Set[UserId]] =
    filters.student(studentId).so(cache.get(studentId))

  private val cache = cacheApi[UserId, Set[UserId]](64, "clas.mates"):
    _.expireAfterWrite(5.minutes)
      .buildAsyncFuture(fetchMatesAndTeachers)

  private def fetchMatesAndTeachers(studentId: UserId): Fu[Set[UserId]] =
    colls.student
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match(bdoc("userId" -> studentId)) -> List(
          Group(BSONNull)("classes" -> PushField("clasId")),
          Facet(
            List(
              "mates" -> List(
                PipelineOperator(
                  $lookup.pipelineFull(
                    from = colls.student.name,
                    as = "mates",
                    let = bdoc("ids" -> "$classes"),
                    pipe = List(
                      bdoc(
                        "$match" -> $expr(
                          $and(
                            bdoc("$in" -> barr("$clasId", "$$ids")),
                            bdoc("$ne" -> barr("$userId", studentId))
                          )
                        )
                      ),
                      bdoc(
                        "$group" -> bdoc(
                          "_id" -> BSONNull,
                          "mates" -> bdoc("$addToSet" -> "$userId")
                        )
                      )
                    )
                  )
                ),
                ReplaceRoot:
                  $ifNull(
                    bdoc("$arrayElemAt" -> barr("$mates", 0)),
                    bdoc("mates" -> barr())
                  )
              ),
              "teachers" -> List(
                PipelineOperator(
                  $lookup.pipelineFull(
                    from = colls.clas.name,
                    as = "teachers",
                    let = bdoc("ids" -> "$classes"),
                    pipe = List(
                      bdoc("$match" -> $expr(bdoc("$in" -> barr("$_id", "$$ids")))),
                      bdoc("$unwind" -> "$teachers"),
                      bdoc(
                        "$group" -> bdoc(
                          "_id" -> BSONNull,
                          "teachers" -> bdoc("$addToSet" -> "$teachers")
                        )
                      )
                    )
                  )
                ),
                ReplaceRoot:
                  $ifNull(
                    bdoc("$arrayElemAt" -> barr("$teachers", 0)),
                    bdoc("teachers" -> barr())
                  )
              )
            )
          ),
          ReplaceRoot:
            bdoc(
              "$mergeObjects" -> barr(
                bdoc("$arrayElemAt" -> barr("$mates", 0)),
                bdoc("$arrayElemAt" -> barr("$teachers", 0))
              )
            )
        )
      .map: docO =>
        for
          doc <- docO
          mates <- doc.getAsOpt[Set[UserId]]("mates")
          teachers <- doc.getAsOpt[Set[UserId]]("teachers")
        yield mates ++ teachers
      .dmap(~_)

  /* Find student that shares a class with me */
  def findMateStudent(studentId: UserId)(using me: Me): Fu[Option[Student]] =
    for
      myClasIds <- colls.clasIdsOfStudent(me.userId)
      mate <- myClasIds.nonEmpty.so:
        colls.student.one[Student](inIds(myClasIds.map(Student.makeId(studentId, _))))
    yield mate
