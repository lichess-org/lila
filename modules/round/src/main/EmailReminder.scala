package lila.round

import lila.db.Types.Coll
import lila.game.Pov
import lila.pref.{ Pref, PrefApi }
import lila.user.UserRepo

import reactivemongo.bson._

private[round] final class EmailReminder(
    coll: Coll,
    prefApi: PrefApi) {

  def apply(pov: Pov): Funit = pov.game.isCorrespondence ?? {
    pov.player.userId ?? { userId =>
      prefApi.getPref(userId, (p: Pref) => p.correspEmail) flatMap {
        case Pref.CorrespEmail.MY_TURN => onMyTurn(pov, userId)
        case _                         => funit
      }
    }
  }

  private def onMyTurn(pov: Pov, userId: String) =

    private def 
}
