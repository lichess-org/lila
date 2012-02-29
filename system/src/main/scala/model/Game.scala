package lila.system
package model

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

case class Game(
    @Key("_id") id: String,
    players: List[Player],
    pgn: String,
    status: Int,
    turns: Int,
    variant: Int) {
}
