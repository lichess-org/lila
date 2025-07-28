package lila.learn

import lila.db.dsl.{ *, given }

final class LearnApi(coll: Coll)(using Executor):

  import LearnHandlers.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    reset(del.id)

  def get(user: UserId): Fu[LearnProgress] =
    coll.one[LearnProgress]($id(user)).dmap { _ | LearnProgress.empty(user.id) }

  private def save(p: LearnProgress): Funit =
    coll.update.one($id(p.id), p, upsert = true).void

  def setScore(user: UserId, stage: String, level: Int, score: StageProgress.Score) =
    get(user).flatMap: prog =>
      save(prog.withScore(stage, level, score))

  def reset(user: UserId) =
    coll.delete.one($id(user)).void

  private val maxCompletion = 110

  def completionPercent(userIds: List[UserId]): Fu[Map[UserId, Int]] =
    coll
      .aggregateList(maxDocs = Int.MaxValue, _.sec): framework =>
        import framework.*
        Match($doc("_id".$in(userIds))) -> List(
          Project($doc("stages" -> $doc("$objectToArray" -> "$stages"))),
          UnwindField("stages"),
          Project(
            $doc(
              "stages" -> $doc(
                "$size" -> $doc(
                  "$filter" -> $doc(
                    "input" -> "$stages.v",
                    "as" -> "s",
                    "cond" -> $doc(
                      "$ne" -> $arr("$$s", 0)
                    )
                  )
                )
              )
            )
          ),
          GroupField("_id")("nb" -> SumField("stages"))
        )
      .map:
        _.view
          .flatMap: obj =>
            (obj.getAsOpt[UserId]("_id"), obj.int("nb")).mapN: (k, v) =>
              k -> (v * 100f / maxCompletion).toInt
          .toMap
