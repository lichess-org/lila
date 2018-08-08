package lidraughts.user

case class UidNb(userId: String, nb: Int)

object UidNb {

  implicit val UidNbBSONHandler = reactivemongo.bson.Macros.handler[UidNb]
}
