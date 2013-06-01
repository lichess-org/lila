package lila.relation

import lila.game.BestOpponents
import lila.user.User

private[relation] final class Autofollow(api: RelationApi) {

  def apply(user: User): Funit = api nbFollowing user.id flatMap { nb ⇒
    (nb == 0) ?? {
      BestOpponents(user.id, 5) flatMap { ops ⇒
        (ops map {
          case (op, _) ⇒ api.autofollow(user.id, op.id)
        }).sequenceFu.void
      }
    }
  }
}
