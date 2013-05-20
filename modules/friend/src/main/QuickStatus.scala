package lila.friend

case class QuickStatus(
    user1: String,
    user2: String,
    friends: Boolean,
    request: Option[Boolean]) {

  def pending = request == Some(true)

  def requested = request == Some(false)
}
