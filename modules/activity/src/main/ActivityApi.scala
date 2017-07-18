package lila.activity

import lila.db.dsl._
import lila.game.Game
import lila.user.User

final class ActivityApi(coll: Coll) {

  import Activity._
  import BSONHandlers._

  def addGame(game: Game): Funit = game.userIds.map { userId =>
    getOrCreate(userId) flatMap { old =>
      ActivityAggregation.addGame(Activity.WithUserId(old, userId), game) ?? save
    }
  }.sequenceFu.void

  def get(userId: User.ID) = coll.byId[Activity, Id](Id today userId)
  def getOrCreate(userId: User.ID) = get(userId) map { _ | Activity.make(userId) }
  def save(activity: Activity) = coll.update($id(activity.id), activity, upsert = true).void
}
