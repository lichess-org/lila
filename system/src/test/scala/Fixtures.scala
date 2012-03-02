package lila.system

import model._

trait Fixtures {

  lazy val newDbGame = DbGame(
    id = "arstdhne",
    players = List(white, black),
    pgn = "",
    status = 10,
    turns = 0,
    variant = 1,
    lastMove = None
  )

  lazy val white = newPlayer("white", "ip ar jp bn kp cb lp dq mp ek np fb op gn pp hr")
  lazy val black = newPlayer("black", "Wp 4r Xp 5n Yp 6b Zp 7q 0p 8k 1p 9b 2p !n 3p ?r")

  def newPlayer(color: String, ps: String) = Player(
    id = color take 4,
    color = color,
    ps = ps,
    aiLevel = None,
    isWinner = None,
    evts = Some("0s|1Msystem White creates the game|2Msystem Black joins the game|3r/ipkkf590ldrr"),
    elo = Some(1280)
  )

  lazy val dbGame1 = DbGame(
    id = "huhuhaha",
    players = List(
      newPlayer("white", "ip ar sp16 sN14 kp ub8 Bp6 dq Kp0 ek np LB12 wp22 Fn2 pp hR"),
      newPlayer("black", "Wp 4r Xp Qn1 Yp LB13 Rp9 hq17 0p 8k 1p 9b 2p sN3 3p ?r")),
    pgn = "e4 Nc6 Nf3 Nf6 e5 Ne4 d3 Nc5 Be3 d6 d4 Ne4 Bd3 Bf5 Nc3 Nxc3 bxc3 Qd7 Bxf5 Qxf5 Nh4 Qe4 g3 Qxh1+",
    status = 31,
    turns = 24,
    variant = 1,
    lastMove = None
  )

  lazy val dbGame2 = DbGame(
    id = "-176b4to",
    players = List(
      newPlayer("white", "zP32 Yr44 jp JN10 Jp20 cb Kp18 KQ2 KP0 Gk30 np ZB22 op QN4 pp dr50"),
      newPlayer("black", "WP Ar19 BP13 QN11 YP ZB35 KP21 KQ3 KP1 Ik37 1p zB29 Ep5 JN9 3p 5r25")),
    pgn = "e4 e5 Qh5 Qf6 Nf3 g6 Qxe5+ Qxe5 Nxe5 Nf6 Nc3 Nc6 Nxc6 bxc6 e5 Nd5 Nxd5 cxd5 d4 Rb8 c3 d6 Be2 dxe5 dxe5 Rg8 Bf3 d4 cxd4 Bb4+ Ke2 g5 a3 g4 Bc6+ Bd7 Bxd7+ Kxd7 axb4 Rxb4 Kd3 Rb3+ Kc4 Rb6 Rxa7 Rc6+ Kb5 Rb8+ Ka5 Rc4 Rd1 Kc6 d5+ Kc5 Rxc7#",
    status = 30,
    turns = 55,
    variant = 1,
    lastMove = Some("a7 c7")
  )
}
