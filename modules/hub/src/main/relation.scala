package lila.hub
package relation

trait RelationApi:
  def fetchAreFriends(u1: UserId, u2: UserId): Fu[Boolean]
