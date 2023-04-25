package lila.study

import cats.data.Validated
import shogi.format.forsyth.Sfen
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.variant.Variant
import play.api.libs.json._

case class AnaUsi(
    usi: Usi,
    sfen: Sfen,
    variant: Variant,
    path: String,
    chapterId: Option[String]
) {

  def node: Validated[String, Node] = {
    shogi.Game(sfen.some, variant)(usi) map { game =>
      Node(
        id = UsiCharPair(usi, variant),
        ply = game.plies,
        usi = usi,
        sfen = game.toSfen,
        check = game.situation.check,
        clock = none,
        children = Node.emptyChildren,
        forceVariation = false
      )
    }
  }
}

object AnaUsi {

  def parse(o: JsObject) = {
    for {
      d <- o obj "d"
      usi <- d str "usi" flatMap { u =>
        shogi.format.usi.Usi.apply(u).orElse(shogi.format.usi.UciToUsi.apply(u))
      }
      sfen <- d str "sfen" map Sfen.apply
      path <- d str "path"
    } yield AnaUsi(
      usi = usi,
      variant = shogi.variant.Variant orDefault ~d.str("variant"),
      sfen = sfen,
      path = path,
      chapterId = d str "ch"
    )
  }
}
