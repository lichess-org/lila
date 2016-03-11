package lila.fishnet

import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

private object BSONHandlers {

  implicit val ClientSkillBSONHandler = new BSONHandler[BSONString, Client.Skill] {
    def read(bsonStr: BSONString): Client.Skill =
      Client.Skill.all.find(_.key == bsonStr.value) err s"Invalid client skill ${bsonStr.value}"
    def write(x: Client.Skill) = BSONString(x.key)
  }

  import Stats.Result
  implicit val StatsResultBSONHandler = Macros.handler[Result]
  implicit val StatsBSONHandler = Macros.handler[Stats]

  implicit val ClientBSONHandler = Macros.handler[Client]

  implicit val InstanceBSONHandler = Macros.handler[Instance]
}
