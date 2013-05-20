package lila.friend

case class Status(
  friendship: Option[Friend], 
  request: Option[Request]) {

}

object Status {

  def apply(friend: Friend): Status = Status(friend.some, none)
  def apply(request: Request): Status = Status(none, request.some)
}
