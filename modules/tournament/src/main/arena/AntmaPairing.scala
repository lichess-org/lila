package lila.tournament
package arena

import scalalib.WMMatching

import lila.common.Chronometer
import lila.tournament.arena.PairingSystem.Data

private object AntmaPairing:

  private val maxStrike = 3

  private type RPlayer = RankedPlayerWithColorHistory

  def apply(data: Data, players: List[RPlayer]): List[Pairing.Prep] =
    players.nonEmpty.so:
      import data.*

      def rankFactor = PairingSystem.rankFactorFor(players)

      def justPlayedTogether(u1: UserId, u2: UserId) =
        lastOpponents.hash.get(u1).contains(u2) || lastOpponents.hash.get(u2).contains(u1)

      def pairScore(a: RPlayer, b: RPlayer): Option[Int] =
        if justPlayedTogether(a.player.userId, b.player.userId) ||
          !a.colorHistory.couldPlay(b.colorHistory, maxStrike)
        then None
        else
          Some:
            Math.abs(a.rank.value - b.rank.value) * rankFactor(a, b) +
              Math.abs(a.player.rating.value - b.player.rating.value)

      def battleScore(a: RPlayer, b: RPlayer): Option[Int] =
        (a.player.team != b.player.team).so(pairScore(a, b))

      def duelScore: (RPlayer, RPlayer) => Option[Int] = (_, _) => Some(1)

      Chronometer.syncMon(_.tournament.pairing.wmmatching):
        WMMatching(
          players.toArray,
          if data.tour.isTeamBattle then battleScore
          else if data.onlyTwoActivePlayers then duelScore
          else pairScore
        ).fold(
          err =>
            logger.error("WMMatching", err)
            Nil
          ,
          _.map: (a, b) =>
            Pairing.prepWithColor(a, b)
        )
