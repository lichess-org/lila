package lila.learn

import reactivemongo.api.ReadPreference
import cats.implicits._

import lila.db.dsl._
import lila.user.User

final class LearnApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def get(user: User): Fu[LearnProgress] =
    coll.one[LearnProgress]($id(user.id)) dmap { _ | LearnProgress.empty(LearnProgress.Id(user.id)) }

  private def save(p: LearnProgress): Funit =
    coll.update.one($id(p.id), p, upsert = true).void.recover(lila.db.ignoreDuplicateKey)

  def setScore(user: User, score: ScoreEntry) =
    get(user) flatMap { prog =>
      save(prog.withScore(score))
    }

  def setScores(user: User, scores: List[ScoreEntry]) =
    get(user) flatMap { prog =>
      val updatedProg = scores.foldLeft(prog) { (acc, score) =>
        acc.withScore(score)
      }
      save(updatedProg)
    }

  def reset(user: User) =
    coll.delete.one($id(user.id)).void

  private val maxCompletion = 123

  def completionPercent(userIds: List[User.ID]): Fu[Map[User.ID, Int]] =
    coll
      .aggregateList(
        maxDocs = Int.MaxValue,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match($doc("_id" $in userIds)) -> List(
          Project($doc("stages" -> $doc("$objectToArray" -> "$stages"))),
          UnwindField("stages"),
          Project(
            $doc(
              "stages" -> $doc(
                "$size" -> $doc(
                  "$filter" -> $doc(
                    "input" -> "$stages.v",
                    "as"    -> "s",
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
      }
      .map {
        _.view
          .flatMap { obj =>
            (obj string "_id", obj int "nb") mapN { (k, v) =>
              k -> (v * 100f / maxCompletion).toInt
            }
          }
          .toMap
      }
}
