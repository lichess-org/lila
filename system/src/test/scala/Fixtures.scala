package lila.system

import model._

trait Fixtures {

  lazy val newGame = Game(
    id = "arstdhne",
    players = List(white, black),
    pgn = "",
    status = 10,
    turns = 0,
    variant = 1
  )

  lazy val white = player("white", "ip ar jp bn kp cb lp dq mp ek np fb op gn pp hr")
  lazy val black = player("black", "Wp 4r Xp 5n Yp 6b Zp 7q 0p 8k 1p 9b 2p !n 3p ?r")

  def player(color: String, ps: String) = Player(
    id = color take 4,
    color = color,
    ps = "ip ar jp bn kp cb lp dq mp ek np fb op gn pp hr",
    aiLevel = None,
    isWinner = None,
    evts = Some("0s|1Msystem White creates the game|2Msystem Black joins the game|3r/ipkkf590ldrr"),
    elo = Some(1280)
  )

}
