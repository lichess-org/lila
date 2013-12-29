package lila.app
package templating

import akka.pattern.ask
import play.api.libs.json._

import lila.api.Context
import lila.relation.Relation
import makeTimeout.short

trait RelationHelper {

  private def api = Env.relation.api

  def relationWith(userId: String)(implicit ctx: Context): Option[Relation] =
    ctx.userId flatMap { api.relation(_, userId).await }

  def followsMe(userId: String)(implicit ctx: Context): Boolean =
    ctx.userId ?? { api.follows(userId, _).await }

  def blocks(u1: String, u2: String): Boolean =
    api.blocks(u1, u2).await

  def nbFollowers(userId: String) =
    Env.relation.api.nbFollowers(userId).await

  def nbFollowing(userId: String) =
    Env.relation.api.nbFollowing(userId).await
}
