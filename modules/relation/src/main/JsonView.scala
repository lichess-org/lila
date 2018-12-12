package lila.relation

import actorApi.OnlineFriends
import play.api.libs.json._

object JsonView {

  implicit def relatedWrites(implicit userWrites: Writes[lila.user.User]) =
    OWrites[Related] { r =>
      Json.obj(
        "user" -> r.user,
        "patron" -> r.user.isPatron,
        "nbGames" -> r.nbGames,
        "followable" -> r.followable,
        "relation" -> r.relation
      )
    }

  def writeOnlineFriends(onlineFriends: OnlineFriends) = {
    // We use 'd' for backward compatibility with the mobile client
    Json.obj(
      "t" -> "following_onlines",
      "d" -> onlineFriends.users.map(_.titleName),
      "playing" -> onlineFriends.playing,
      "studying" -> onlineFriends.studying,
      "patrons" -> onlineFriends.patrons
    )
  }

  def writeFriendEntering(friendEntering: FriendEntering) = {
    // We use 'd' for backward compatibility with the mobile client
    Json.obj(
      "t" -> "following_enters",
      "d" -> friendEntering.user.titleName
    )
      .add("playing" -> friendEntering.isPlaying)
      .add("studying" -> friendEntering.isStudying)
      .add("patron" -> friendEntering.user.isPatron)
  }
}
