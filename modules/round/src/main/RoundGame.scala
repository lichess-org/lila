package lila.round

import chess.{ Centis, Color }

import lila.core.game.{ Player, Source }

object RoundGame:

  extension (g: Game)

    def playableBy(p: Player): Boolean = g.playable && p == g.player
    def playableBy(c: Color): Boolean = g.playable && g.turnOf(c)
    def playableByAi: Boolean = g.playable && g.player.isAi

    def mobilePushable: Boolean = g.isCorrespondence && g.playable && g.nonAi
    def alarmable: Boolean = g.hasCorrespondenceClock && g.playable && g.nonAi

    def hasChat = !g.isTournament && !g.isSimul && !g.isSwiss && g.nonAi

    def moretimeable(color: Color) =
      g.playable && g.canTakebackOrAddTime && !g.hasRule(_.noGiveTime) && {
        g.clock.exists(_.moretimeable(color)) || g.correspondenceClock.exists(_.moretimeable(color))
      }
    def forceDrawable = g.playable && g.nonAi && !g.abortable && !g.isSwiss && !g.hasRule(_.noClaimWin)

    def isSwitchable = g.nonAi && (g.isCorrespondence || g.isSimul)

    def secondsSinceCreation = (nowSeconds - g.createdAt.toSeconds).toInt

    def justCreated = g.secondsSinceCreation < 2

    def timeForFirstMove: Centis =
      Centis.ofSeconds:
        import chess.Speed.*
        val base =
          if g.isTournament then
            g.speed match
              case UltraBullet => 11
              case Bullet => 16
              case Blitz => 21
              case Rapid => 25
              case _ => 30
          else
            g.speed match
              case UltraBullet => 15
              case Bullet => 20
              case Blitz => 25
              case Rapid => 30
              case _ => 35
        if g.variant.chess960 then base * 5 / 4
        else base

    def expirable =
      !g.bothPlayersHaveMoved &&
        g.source.exists(Source.expirable.contains) &&
        g.playable &&
        g.nonAi &&
        g.clock.exists(!_.isRunning)

    def timeBeforeExpiration: Option[Centis] = g.expirable.option:
      Centis.ofMillis(g.movedAt.toMillis - nowMillis + g.timeForFirstMove.millis).nonNeg
