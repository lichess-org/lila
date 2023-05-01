package lila.fishnet

import org.specs2.mutable._
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi._

final class KyotoFairyTest extends Specification {

  "convert game to fairy format" in {

    val g = JsonApi.fromGame(
      Work.Game(
        id = "",
        initialSfen = None,
        studyId = None,
        variant = shogi.variant.Kyotoshogi,
        moves = "4e5d+ 2a1b+ 5e4d+ 3a2b 2e3d+ 2b3a 1e1d+"
      )
    )
    g.position.truncate.value must_== "p+nks+l/5/5/5/+LSK+NP b -"
    g.moves must_== "4e5d+ 2a1b+ 5e4d- 3a2b 2e3d- 2b3a 1e1d+"

  }

  "sfen" in {
    JsonApi.Kyoto.fairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b -")) must_== Sfen("p+nks+l/5/5/L+S1N+P/+LSK+NP b -")
    JsonApi.Kyoto.fairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP w -")) must_== Sfen("p+nks+l/5/5/L+S1N+P/+LSK+NP w -")
    JsonApi.Kyoto.fairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b PGTS")) must_== Sfen(
      "p+nks+l/5/5/L+S1N+P/+LSK+NP b PNLS"
    )
    JsonApi.Kyoto.fairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b pgts")) must_== Sfen(
      "p+nks+l/5/5/L+S1N+P/+LSK+NP b pnls"
    )
    JsonApi.Kyoto.fairySfen(Sfen("pgkst/5/5/LB1NR/TSKGP b P2GTSpgts")) must_== Sfen(
      "p+nks+l/5/5/L+S1N+P/+LSK+NP b P2NLSpnls"
    )
  }

  "fairyToLishogi drops" in {
    JsonApi.Kyoto.fairyToLishogi("+L*5a") must_== "T*5a"
    JsonApi.Kyoto.fairyToLishogi("L*5a") must_== "L*5a"
    JsonApi.Kyoto.fairyToLishogi("+P*5a") must_== "R*5a"
    JsonApi.Kyoto.fairyToLishogi("P*5a") must_== "P*5a"
    JsonApi.Kyoto.fairyToLishogi("+S*5a") must_== "B*5a"
    JsonApi.Kyoto.fairyToLishogi("S*5a") must_== "S*5a"
    JsonApi.Kyoto.fairyToLishogi("+N*5a") must_== "G*5a"
    JsonApi.Kyoto.fairyToLishogi("N*5a") must_== "N*5a"
  }

  "fairyToLishogi moves" in {
    JsonApi.Kyoto.fairyToLishogi("1e1d+") must_== "1e1d+"
    JsonApi.Kyoto.fairyToLishogi("5e5d-") must_== "5e5d+"
    JsonApi.Kyoto.fairyToLishogi("3e4d") must_== "3e4d"
  }

  "lishogiToFairy drops" in {
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("T*5a").get, Tokin)) must_== "+L*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("L*5a").get, Lance)) must_== "L*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("R*5a").get, Rook)) must_== "+P*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("P*5a").get, Pawn)) must_== "P*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("B*5a").get, Bishop)) must_== "+S*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("S*5a").get, Silver)) must_== "S*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("G*5a").get, Gold)) must_== "+N*5a"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("N*5a").get, Knight)) must_== "N*5a"
  }

  "lishogiToFairy moves" in {
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b").get, King)) must_== "3a2b"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Silver)) must_== "3a2b+"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Bishop)) must_== "3a2b-"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Lance)) must_== "3a2b+"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Tokin)) must_== "3a2b-"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Knight)) must_== "3a2b+"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Gold)) must_== "3a2b-"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Pawn)) must_== "3a2b+"
    JsonApi.Kyoto.lishogiToFairy(Usi.WithRole(Usi("3a2b+").get, Rook)) must_== "3a2b-"
  }

}
