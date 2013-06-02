package lila.relation

import lila.game.BestOpponents
import lila.user.User

private[relation] final class Autofollow(api: RelationApi) {

  private val max = 5

  def apply(user: User): Funit = api nbFollowing user.id flatMap { nb ⇒
    (nb == 0) ?? {
      BestOpponents(user.id, max * 2) flatMap { ops ⇒
        (ops take max map {
          case (op, _) ⇒ api.autofollow(user.id, op.id)
        }).sequenceFu.void
      }
    }
  }
}
