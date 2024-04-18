package lila.game
package core

import lila.core.game.Game

type OnTvGame = Game => Unit

object insight:

  trait InsightDb

  trait InsightApi:
    def indexAll(user: lila.core.user.User): Funit
