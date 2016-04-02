package lila.team

private object BSONHandlers {

  import lila.db.dsl.BSONJodaDateTimeHandler
  implicit val TeamBSONHandler = reactivemongo.bson.Macros.handler[Team]
  implicit val RequestBSONHandler = reactivemongo.bson.Macros.handler[Request]
  implicit val MemberBSONHandler = reactivemongo.bson.Macros.handler[Member]
}
