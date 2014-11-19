package lila.relay

import org.specs2.mutable._
import org.specs2.specification._

final class ParserTest extends Specification {

  val str = """
You will not see seek ads.
fics% You will not hear shouts.
fics% You will not hear cshouts.
fics% You will not hear kibitzes.
fics% Style 12 set.
fics% You are now observing game 250.
Game 250: MasterBing (1995) Maurian (2035) rated blitz 3 0

<12> r---r-k- ppp--pbp --np--p- -------- --PNP--q -P--BP-- P-----PP --RQ-RK- W -1 0 0 0 0 1 250 MasterBing Maurian 0 3 0 32 32 144 154 17 R/f8-e8 (0:03) Rfe8 0 1 0
fics%
Movelist for game 250:

MasterBing (1995) vs. Maurian (2035) --- Tue Nov 18, 15:30 PST 2014
Rated blitz match, initial time: 3 minutes, increment: 0 seconds.

Move  MasterBing         Maurian
----  ----------------   ----------------
  1.  d4      (0:00)     Nf6     (0:00)
  2.  c4      (0:01)     d6      (0:01)
  3.  Nc3     (0:02)     g6      (0:00)
  4.  e4      (0:01)     Bg7     (0:00)
  5.  Nf3     (0:01)     Bg4     (0:01)
  6.  Be2     (0:01)     O-O     (0:01)
  7.  O-O     (0:01)     Nfd7    (0:01)
  8.  Be3     (0:02)     Nc6     (0:01)
  9.  Nd2     (0:03)     Bxe2    (0:02)
 10.  Nxe2    (0:00)     e5      (0:01)
 11.  Nb3     (0:03)     exd4    (0:02)
 12.  Nbxd4   (0:02)     Nde5    (0:01)
 13.  Rc1     (0:03)     Nxd4    (0:04)
 14.  Nxd4    (0:03)     Nc6     (0:01)
 15.  b3      (0:08)     Qh4     (0:05)
 16.  f3      (0:05)     Rfe8    (0:03)
      {Still in progress} *

fics% """

  "convert FICS string to a game" should {
    "work :)" in {
      Parser.pgn(str).pgn.size must_== 445
    }
  }
}

