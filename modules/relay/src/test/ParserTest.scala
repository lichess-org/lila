package lila.relay

import org.specs2.mutable._
import org.specs2.specification._

final class ParserTest extends Specification {

  val str = """Style 12 set.
fics% You are now observing game 277.
Game 277: depop (1824) Sheikki (2194) rated lightning 1 0

<12> rn-qkb-r ppp--ppp -------- ---n---- ----p--- --N----- PPPPQPPP R-B-K-NR W -1 1 1 1 1 0 277 depop Sheikki 0 1 0 35 35 51 59 8 N/f6-d5
(0:00) Nxd5 0 1 0
fics%
Movelist for game 277:

depop (1824) vs. Sheikki (2194) --- Tue Nov 18, 08:32 PST 2014
Rated lightning match, initial time: 1 minutes, increment: 0 seconds.

Move  depop              Sheikki
----  ----------------   ----------------
  1.  e4      (0:00)     e5      (0:00)
  2.  Nf3     (0:00)     d5      (0:00)
  3.  Nc3     (0:01)     Nf6     (0:00)
  4.  exd5    (0:03)     e4      (0:00)
  5.  Ng1     (0:01)     Bg4     (0:00)
  6.  Be2     (0:01)     Bxe2    (0:00)
  7.  Qxe2    (0:02)     Nxd5    (0:00)
      {Still in progress} *

fics% """

  "convert FICS string to a game" should {
    "work :)" in {
      Parser.game(str) must beSome.like {
        case g =>
          g.whitePlayer.name must_== "depop (1824)".some
          g.blackPlayer.name must_== "Sheikki (2194)".some
          g.pgnImport.flatMap(_.user) must_== "lichess-replay".some
          g.status must_== chess.Status.Started
          g.turns must_== 7
          g.player must_== chess.Color.White
      }
    }
  }
}

