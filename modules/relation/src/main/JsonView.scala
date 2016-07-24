package lila.relation

import lila.common.PimpedJson._
import lila.hub.actorApi.relation.OnlineFriends
import play.api.libs.json._

object JsonView {

  implicit def relatedWrites(implicit userWrites: Writes[lila.user.User]) =
    OWrites[Related] { r =>
      Json.obj(
        "user" -> r.user,
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
      "patrons" -> onlineFriends.patrons)
  }

  def writeFriendEntering(friendEntering: FriendEntering) = {
    // We use 'd' for backward compatibility with the mobile client
    Json.obj(
      "t" -> "following_enters",
      "d" -> friendEntering.user.titleName,
      "playing" -> friendEntering.isPlaying,
      "patron" -> friendEntering.user.isPatron
    )
  }
}
