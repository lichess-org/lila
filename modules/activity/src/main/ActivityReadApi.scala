package lila.activity

import lila.analyse.Analysis
import lila.db.dsl._
import lila.game.Game
import lila.user.User
import lila.user.UserRepo.lichessId

final class ActivityReadApi(
    coll: Coll,
    practiceApi: lila.practice.PracticeApi
) {

  import Activity._
  import BSONHandlers._
  import activities._

  def recent(userId: User.ID, days: Int): Fu[List[ActivityView.AsTo]] = for {
    as <- coll.byOrderedIds[Activity, Id](makeIds(userId, days))(_.id)
    practiceStructure <- as.exists(_.practice.isDefined) ?? {
      practiceApi.structure.get map some
    }
  } yield as.map { a =>

    val practice = for {
      p <- a.practice
      struct <- practiceStructure
    } yield p.value flatMap {
      case (studyId, nb) => struct study studyId map (_ -> nb)
    } toMap

    val view = ActivityView(
      games = a.games,
      puzzles = a.puzzles,
      practice = practice
    )

    ActivityView.AsTo(a.date, view)
  }

  private def makeIds(userId: User.ID, days: Int): List[Id] =
    Day.recent(days).map { Id(userId, _) }
}
