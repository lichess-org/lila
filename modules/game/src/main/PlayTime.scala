package lila.game

import lila.db.dsl._
import lila.db.ByteArray
import lila.user.{ User, UserRepo }

import reactivemongo.bson._

final class PlayTime(gameColl: Coll) {

  private val clockTimesField = Game.BSONFields.clockTimes
  private val tvField = Game.BSONFields.tvAt

  def apply(user: User): Fu[User.PlayTime] = user.playTime match {
    case Some(pt) => fuccess(pt)
    case _ => {
      gameColl
        .find($doc(
          Game.BSONFields.playerUids -> user.id,
          Game.BSONFields.status -> $doc("$gte" -> chess.Status.Mate.id)
        ))
        .projection($doc(
          clockTimesField -> true,
          tvField -> true
        ))
        .cursor[Bdoc]().fold(User.PlayTime(0, 0)) { (pt, doc) =>
        val t = doc.getAs[ByteArray](clockTimesField) ?? { times =>
          val t = BinaryFormat.clockTimes.read(times)
          (t, t drop 1).zipped.map(_ - _).sum
        } / 100
        val isTv = doc.get(tvField).isDefined
        User.PlayTime(pt.total + t, pt.tv + isTv.fold(t, 0))
      }
    }.addEffect { UserRepo.setPlayTime(user, _) }
  }
}
