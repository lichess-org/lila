package lila.system

import ornicar.scalalib.{IntStatus, IntStatuses}

sealed case class GameStatus(id: Int, name: String) extends IntStatus
sealed case class GameVariant(id: Int, name: String) extends IntStatus

object GameStatuses extends IntStatuses[GameStatus] {

  val values = Set(
    GameStatus(10, "created"),
    GameStatus(20, "started"),
    GameStatus(25, "aborted"),
    GameStatus(30, "mate"),
    GameStatus(31, "resign"),
    GameStatus(32, "stalemate"),
    GameStatus(33, "timeout"),
    GameStatus(34, "draw"),
    GameStatus(35, "outoftime"),
    GameStatus(36, "cheat")
  )
}

object GameVariants extends IntStatuses[GameVariant] {

  val values = Set(
    GameVariant(1, "variant_standard"),
    GameVariant(2, "variant_960")
  )
}
