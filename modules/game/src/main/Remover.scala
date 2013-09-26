package lila.game

import lila.db.api._
import lila.hub.actorApi.bookmark.Remove
import tube.gameTube

private[game] final class Remover(bookmark: akka.actor.ActorSelection) {

  def apply(ids: List[GameRepo.ID]) {
    $remove[Game]($select byIds ids)
    PgnRepo removeIds ids
    bookmark ! Remove(ids)
  }

  def apply(id: GameRepo.ID) {
    apply(id :: Nil)
  }
}
