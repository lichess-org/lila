package lila.fishnet

import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

private object BSONHandlers {

  implicit val ClientKeyBSONHandler = new BSONHandler[BSONString, Client.Key] {
    def read(x: BSONString) = Client.Key(x.value)
    def write(x: Client.Key) = BSONString(x.value)
  }
  implicit val ClientVersionBSONHandler = new BSONHandler[BSONString, Client.Version] {
    def read(x: BSONString) = Client.Version(x.value)
    def write(x: Client.Version) = BSONString(x.value)
  }
  implicit val ClientUserIdBSONHandler = new BSONHandler[BSONString, Client.UserId] {
    def read(x: BSONString) = Client.UserId(x.value)
    def write(x: Client.UserId) = BSONString(x.value)
  }
  implicit val ClientSkillBSONHandler = new BSONHandler[BSONString, Client.Skill] {
    def read(x: BSONString) = Client.Skill byKey x.value err s"Invalid client skill ${x.value}"
    def write(x: Client.Skill) = BSONString(x.key)
  }

  import Client.Engine
  implicit val EngineBSONHandler = Macros.handler[Engine]

  import Client.Instance
  implicit val InstanceBSONHandler = Macros.handler[Instance]

  import Stats.Result
  implicit val StatsResultBSONHandler = Macros.handler[Result]
  implicit val StatsBSONHandler = Macros.handler[Stats]

  implicit val ClientBSONHandler = Macros.handler[Client]
}
