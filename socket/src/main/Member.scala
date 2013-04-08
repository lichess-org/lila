package lila.socket

trait Member {

  val channel: JsChannel
  val userId: Option[String]

  private val privateLiveGames = collection.mutable.Set[String]()

  def liveGames: Set[String] = privateLiveGames.toSet

  def addLiveGames(ids: List[String]) { ids foreach privateLiveGames.+= }
}

object Member {

  def apply(c: JsChannel): Member = apply(c, none)

  def apply(c: JsChannel, uid: Option[String]): Member = new Member {
    val channel = c
    val userId = uid
  }
}
