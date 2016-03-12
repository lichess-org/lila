package lila.fishnet

import reactivemongo.bson._

import lila.db.Implicits._

private final class Limiter(analysisColl: Coll) {

  def apply(sender: Work.Sender): Fu[Boolean] = sender match {
    case Work.Sender(_, _, mod, system) if (mod || system) => fuccess(true)
    case Work.Sender(Some(userId), _, _, _) => analysisColl.count(BSONDocument(
      "sender.userId" -> userId
    ).some) map (0 ==)
    case Work.Sender(_, Some(ip), _, _) => analysisColl.count(BSONDocument(
      "sender.ip" -> ip
    ).some) map (0 ==)
    case _ => fuccess(false)
  }
}
