package lila.msg

import org.joda.time.DateTime
import scala.concurrent.duration._
import reactivemongo.api._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class MsgApi(
    colls: MsgColls,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def inbox(me: User): Fu[List[MsgThread]] =
    colls.thread.ext
      .find(
        $doc(
          "users" -> me.id,
          "blockers" $ne me.id
        )
      )
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](100)
}
