package lila.activity

import lila.analyse.Analysis
import lila.db.dsl._
import lila.game.Game
import lila.user.User
import lila.user.UserRepo.lichessId

final class ActivityApi(coll: Coll) {

  import Activity._
  import BSONHandlers._

  def get(userId: User.ID) = coll.byId[Activity, Id](Id today userId)

  def addGame(game: Game): Funit = game.userIds.map { userId =>
    update(userId) { ActivityAggregation.addGame(game, userId) _ }
  }.sequenceFu.void

  def addAnalysis(analysis: Analysis): Funit = analysis.uid.filter(lichessId !=) ?? { userId =>
    update(userId) { ActivityAggregation.addAnalysis(analysis) _ }
  }

  def addForumPost(post: lila.forum.Post, topic: lila.forum.Topic): Funit = post.userId.filter(lichessId !=) ?? { userId =>
    update(userId) { ActivityAggregation.addForumPost(post, topic) _ }
  }

  def addPuzzle(puzzleId: lila.puzzle.PuzzleId, userId: User.ID, result: lila.puzzle.Result): Funit =
    update(userId) { ActivityAggregation.addPuzzle(puzzleId, result) _ }

  private def getOrCreate(userId: User.ID) = get(userId) map { _ | Activity.make(userId) }
  private def save(activity: Activity) = coll.update($id(activity.id), activity, upsert = true).void
  private def update(userId: User.ID)(f: Activity => Option[Activity]): Funit =
    getOrCreate(userId) flatMap { old =>
      f(old) ?? save
    }
}
