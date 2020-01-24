package lila.msg

import lila.db.dsl._
import reactivemongo.api.bson._

private[msg] object BsonHandlers {

  import Msg.Last
  implicit val msgContentBSONHandler = Macros.handler[Last]

  implicit val threadIdBSONHandler = stringAnyValHandler[MsgThread.Id](_.value, MsgThread.Id.apply)
  implicit val threadBSONHandler   = Macros.handler[MsgThread]

  implicit val msgIdBSONHandler = stringAnyValHandler[Msg.Id](_.value, Msg.Id.apply)
  implicit val msgBSONHandler   = Macros.handler[Msg]
}
