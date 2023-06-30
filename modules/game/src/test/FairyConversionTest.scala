package lila.game

import org.specs2.mutable._
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi._

final class FairyConversionTest extends Specification {
  import FairyConversion._
  "sfen" in {
    Kyoto.makeFairySfen(shogi.variant.Kyotoshogi.initialSfen) must_== Sfen("p+nks+l/5/5/5/+LSK+NP b -")
    Kyoto.makeFairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b -")) must_== Sfen("p+nks+l/5/5/L+S1N+P/+LSK+NP b -")
    Kyoto.makeFairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP w -")) must_== Sfen("p+nks+l/5/5/L+S1N+P/+LSK+NP w -")
    Kyoto.makeFairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b PGTS")) must_== Sfen(
      "p+nks+l/5/5/L+S1N+P/+LSK+NP b PNLS"
    )
    Kyoto.makeFairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b pgts")) must_== Sfen(
      "p+nks+l/5/5/L+S1N+P/+LSK+NP b pnls"
    )
    Kyoto.makeFairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b P2GTSpgts")) must_== Sfen(
      "p+nks+l/5/5/L+S1N+P/+LSK+NP b P2NLSpnls"
    )
  }

  "fairyToUsi drops" in {
    Kyoto.fairyToUsi("+L*5a") must_== "T*5a"
    Kyoto.fairyToUsi("L*5a") must_== "L*5a"
    Kyoto.fairyToUsi("+P*5a") must_== "R*5a"
    Kyoto.fairyToUsi("P*5a") must_== "P*5a"
    Kyoto.fairyToUsi("+S*5a") must_== "B*5a"
    Kyoto.fairyToUsi("S*5a") must_== "S*5a"
    Kyoto.fairyToUsi("+N*5a") must_== "G*5a"
    Kyoto.fairyToUsi("N*5a") must_== "N*5a"
  }

  "fairyToUsi moves" in {
    Kyoto.fairyToUsi("1e1d+") must_== "1e1d+"
    Kyoto.fairyToUsi("5e5d-") must_== "5e5d+"
    Kyoto.fairyToUsi("3e4d") must_== "3e4d"
  }

  "usiWithRoleToFairy drops" in {
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("T*5a").get, Tokin)) must_== "+L*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("L*5a").get, Lance)) must_== "L*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("R*5a").get, Rook)) must_== "+P*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("P*5a").get, Pawn)) must_== "P*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("B*5a").get, Bishop)) must_== "+S*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("S*5a").get, Silver)) must_== "S*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("G*5a").get, Gold)) must_== "+N*5a"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("N*5a").get, Knight)) must_== "N*5a"
  }

  "usiWithRoleToFairy moves" in {
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b").get, King)) must_== "3a2b"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Silver)) must_== "3a2b+"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Bishop)) must_== "3a2b-"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Lance)) must_== "3a2b+"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Tokin)) must_== "3a2b-"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Knight)) must_== "3a2b+"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Gold)) must_== "3a2b-"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Pawn)) must_== "3a2b+"
    Kyoto.usiWithRoleToFairy(Usi.WithRole(Usi("3a2b+").get, Rook)) must_== "3a2b-"
    Kyoto
      .makeFairyUsiList(
        Usi.readList("4e5d+ 2a1b+ 5e4d+ 3a2b 2e3d+ 2b3a 1e1d+").get,
        None
      )
      .mkString(" ") must_== "4e5d+ 2a1b+ 5e4d- 3a2b 2e3d- 2b3a 1e1d+"
  }

}
