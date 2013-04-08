package lila.socket

trait SocketMember {

  val channel: JsChannel
  val userId: Option[String]

  private val privateLiveGames = collection.mutable.Set[String]()

  def liveGames: Set[String] = privateLiveGames.toSet

  def addLiveGames(ids: List[String]) { ids foreach privateLiveGames.+= }
}

object SocketMember {

  def apply(c: JsChannel): SocketMember = apply(c, none)

  def apply(c: JsChannel, uid: Option[String]): SocketMember = new SocketMember {
    val channel = c
    val userId = uid
  }
}
