package lila.activity

import lila.analyse.Analysis
import lila.db.dsl._
import lila.game.Game
import lila.practice.PracticeStructure
import lila.user.User
import lila.user.UserRepo.lichessId

final class ActivityReadApi(
    coll: Coll,
    practiceApi: lila.practice.PracticeApi,
    postApi: lila.forum.PostApi
) {

  import Activity._
  import BSONHandlers._
  import activities._

  def recent(userId: User.ID, days: Int): Fu[List[ActivityView.AsTo]] = for {
    as <- coll.byOrderedIds[Activity, Id](makeIds(userId, days))(_.id)
    practiceStructure <- as.exists(_.practice.isDefined) ?? {
      practiceApi.structure.get map some
    }
    views <- as.map { one(_, practiceStructure) }.sequenceFu
  } yield views

  private def one(a: Activity, practiceStructure: Option[PracticeStructure]): Fu[ActivityView.AsTo] = for {
    posts <- a.posts ?? { p =>
      postApi.liteViewsByIds(p.value.map(_.value)) dmap some
    }
    practice = (for {
      p <- a.practice
      struct <- practiceStructure
    } yield p.value flatMap {
      case (studyId, nb) => struct study studyId map (_ -> nb)
    } toMap)
    postView = posts.map { p =>
      p.groupBy(_.topic).mapValues { posts =>
        posts.map(_.post).sortBy(_.createdAt)
      }
    }
    view = ActivityView(
      games = a.games,
      puzzles = a.puzzles,
      practice = practice,
      posts = postView,
      patron = a.patron
    )
  } yield ActivityView.AsTo(a.date, view)

  private def makeIds(userId: User.ID, days: Int): List[Id] =
    Day.recent(days).map { Id(userId, _) }
}
