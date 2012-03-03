package lila.system

import model._

trait Fixtures {

  lazy val newDbGame = DbGame(
    id = "arstdhne",
    players = List(white, black),
    pgn = "",
    status = 10,
    turns = 0,
    lastMove = None,
    clock = None
  )

  lazy val white = newDbPlayer("white", "ip ar jp bn kp cb lp dq mp ek np fb op gn pp hr")
  lazy val black = newDbPlayer("black", "Wp 4r Xp 5n Yp 6b Zp 7q 0p 8k 1p 9b 2p !n 3p ?r")

  def newDbPlayer(color: String, ps: String) = DbPlayer(
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
      newDbPlayer("white", "ip ar sp16 sN14 kp ub8 Bp6 dq Kp0 ek np LB12 wp22 Fn2 pp hR"),
      newDbPlayer("black", "Wp 4r Xp Qn1 Yp LB13 Rp9 hq17 0p 8k 1p 9b 2p sN3 3p ?r")),
    pgn = "e4 Nc6 Nf3 Nf6 e5 Ne4 d3 Nc5 Be3 d6 d4 Ne4 Bd3 Bf5 Nc3 Nxc3 bxc3 Qd7 Bxf5 Qxf5 Nh4 Qe4 g3 Qxh1+",
    status = 31,
    turns = 24,
    clock = None,
    lastMove = None
  )

// "clock" : {
//"color" : "black",
//"increment" : 5,
//"limit" : 1200,
//"times" : {
        //"white" : 196.25,
        //"black" : 304.1
//}
//}
  lazy val dbGame2 = DbGame(
    id = "-176b4to",
    players = List(
      newDbPlayer("white", "zP32 Yr44 jp JN10 Jp20 cb Kp18 KQ2 KP0 Gk30 np ZB22 op QN4 pp dr50"),
      newDbPlayer("black", "WP Ar19 BP13 QN11 YP ZB35 KP21 KQ3 KP1 Ik37 1p zB29 Ep5 JN9 3p 5r25")),
    pgn = "e4 e5 Qh5 Qf6 Nf3 g6 Qxe5+ Qxe5 Nxe5 Nf6 Nc3 Nc6 Nxc6 bxc6 e5 Nd5 Nxd5 cxd5 d4 Rb8 c3 d6 Be2 dxe5 dxe5 Rg8 Bf3 d4 cxd4 Bb4+ Ke2 g5 a3 g4 Bc6+ Bd7 Bxd7+ Kxd7 axb4 Rxb4 Kd3 Rb3+ Kc4 Rb6 Rxa7 Rc6+ Kb5 Rb8+ Ka5 Rc4 Rd1 Kc6 d5+ Kc5 Rxc7#",
    status = 30,
    turns = 55,
    clock = DbClock(
      color = "black",
      increment = 5,
      limit = 1200,
      times = Map(
        "white" -> 196.25f,
        "black" -> 304.1f
      )
    ).some,
    lastMove = Some("a7 c7")
  )

  // { "_id" : "7xfxoj4v", "clock" : null, "createdAt" : ISODate("2012-01-28T01:55:33Z"), "creatorColor" : "black", "initialFen" : "rkbbnnqr/pppppppp/8/8/8/8/PPPPPPPP/RKBBNNQR w KQkq - 0 1", "lastMove" : "a3 a8", "pgn" : "d4 d5 f3 Bf5 Ne3 Nd6 Bd2 c6 g4 Bb6 gxf5 Nd7 Qg5 f6 Qg4 h5 Qh4 Rh6 N1g2 Rg6 Qf2 Rg5 c3 e6 Kc1 exf5 Kb1 f4 Nxf4 Nf5 h4 Nxe3 hxg5 Nxd1 Qf1 Nxc3+ Bxc3 a5 Qf2 Nc5 Kc1 Ra6 Rh2 fxg5 dxc5 gxf4 cxb6 Rxb6 Rxh5 g6 Qxb6 Qe6 Qd8+ Qc8 Qxc8+ Kxc8 Rh2 a4 Kc2 b5 Rh7 c5 Bg7 a3 b3 c4 Bf6 cxb3+ axb3 b4 Be5 g5 Bd6 Kd8 Bxb4 d4 Rxa3 d3+ exd3 g4 Ra8#", "players" : [     {     "aiLevel" : 1,   "color" : "white",      "id" : "jqsx",  "isAi" : true,  "isWinner" : true,      "ps" : "zb6 dB 6Q12 uN4 DN18 4r76 kk24 3r42 rp68 rP64 sP22 PP0 tp78 vp2 LP8 MP30" },    {     "color" : "black",       "id" : "7n7r",  "ps" : "LB3 PB9 6Q51 IN11 sN5 PR41 7k55 MR17 qP37 zP59 rP7 tP1 DP23 Dp13 Ep49 NP15" } ], "status" : 30, "turns" : 81, "updatedAt" : ISODate("2012-01-28T02:01:28Z"), "userIds" : [ ], "variant" : 2, "winnerUserId" : "" }
  lazy val dbGame3 = DbGame(
    id = "7xfxoj4v",
    players = List(
      newDbPlayer("white", "zb6 dB 6Q12 uN4 DN18 4r76 kk24 3r42 rp68 rP64 sP22 PP0 tp78 vp2 LP8 MP30"),
      newDbPlayer("black", "LB3 PB9 6Q51 IN11 sN5 PR41 7k55 MR17 qP37 zP59 rP7 tP1 DP23 Dp13 Ep49 NP15")),
    pgn = "d4 d5 f3 Bf5 Ne3 Nd6 Bd2 c6 g4 Bb6 gxf5 Nd7 Qg5 f6 Qg4 h5 Qh4 Rh6 N1g2 Rg6 Qf2 Rg5 c3 e6 Kc1 exf5 Kb1 f4 Nxf4 Nf5 h4 Nxe3 hxg5 Nxd1 Qf1 Nxc3+ Bxc3 a5 Qf2 Nc5 Kc1 Ra6 Rh2 fxg5 dxc5 gxf4 cxb6 Rxb6 Rxh5 g6 Qxb6 Qe6 Qd8+ Qc8 Qxc8+ Kxc8 Rh2 a4 Kc2 b5 Rh7 c5 Bg7 a3 b3 c4 Bf6 cxb3+ axb3 b4 Be5 g5 Bd6 Kd8 Bxb4 d4 Rxa3 d3+ exd3 g4 Ra8#",
    status = 30,
    turns = 81,
    clock = None,
    lastMove = Some("a3 a8")
  )

}
