package lila.game

import lila.db.api._
import lila.db.ByteArray
import lila.user.{ User, UserRepo }
import org.joda.time.Period
import tube.gameTube

import play.api.libs.iteratee.Iteratee
import reactivemongo.bson._

object PlayTime {

  private val moveTimeField = Game.BSONFields.moveTimes
  private val tvField = Game.BSONFields.tvAt

  def apply(user: User): Fu[User.PlayTime] = user.playTime match {
    case Some(pt) => fuccess(pt)
    case None => {
      gameTube.coll
        .find(BSONDocument(
          Game.BSONFields.playerUids -> user.id,
          Game.BSONFields.status -> BSONDocument("$gte" -> chess.Status.Mate.id)
        ))
        .projection(BSONDocument(
          moveTimeField -> true,
          tvField -> true
        ))
        .cursor[BSONDocument]
        .enumerate() |>>> (Iteratee.fold(User.PlayTime(0, 0)) {
          case (pt, doc) =>
            val t = doc.getAs[ByteArray](moveTimeField) ?? { times =>
              BinaryFormat.moveTime.read(times).sum
            } / 10
            val isTv = doc.get(tvField).isDefined
            User.PlayTime(pt.total + t, pt.tv + isTv.fold(t, 0))
        })
    }.addEffect { UserRepo.setPlayTime(user, _) }
  }
}
