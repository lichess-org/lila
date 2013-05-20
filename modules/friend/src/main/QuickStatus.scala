package lila.friend

case class QuickStatus(
    user1: String,
    user2: String,
    friends: Boolean,
    request: Option[Boolean]) {

  def requested = ~request

  def pending = request == Some(false)
}
