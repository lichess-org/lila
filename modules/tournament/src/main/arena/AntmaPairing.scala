package lila.tournament
package arena

import lila.common.{ Chronometer, WMMatching }
import lila.user.User
import PairingSystem.Data

private object AntmaPairing {

  private[this] val maxStrike = 3

  private type RPlayer = RankedPlayerWithColorHistory

  def apply(data: Data, players: List[RPlayer]): List[Pairing.Prep] =
    players.nonEmpty ?? {
      import data._

      def rankFactor = PairingSystem.rankFactorFor(players)

      def justPlayedTogether(u1: User.ID, u2: User.ID) =
        lastOpponents.hash.get(u1).contains(u2) ||
          lastOpponents.hash.get(u2).contains(u1)

      def pairScore(a: RPlayer, b: RPlayer): Option[Int] =
        if (
          justPlayedTogether(a.player.userId, b.player.userId) ||
          !a.colorHistory.couldPlay(b.colorHistory, maxStrike)
        ) None
        else
          Some {
            Math.abs(a.rank - b.rank) * rankFactor(a, b) +
              Math.abs(a.player.rating - b.player.rating)
          }

      def battleScore(a: RPlayer, b: RPlayer): Option[Int] =
        (a.player.team != b.player.team) ?? pairScore(a, b)

      def duelScore: (RPlayer, RPlayer) => Option[Int] = (_, _) => Some(1)

      Chronometer.syncMon(_.tournament.pairing.wmmatching) {
        WMMatching(
          players.toArray,
          if (data.tour.isTeamBattle) battleScore
          else if (data.onlyTwoActivePlayers) duelScore
          else pairScore
        ).fold(
          err => {
            logger.error("WMMatching", err)
            Nil
          },
          _ map {
            case (a, b) => Pairing.prepWithColor(tour, a, b)
          }
        )
      }
    }
}
