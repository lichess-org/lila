package lidraughts.socket

import draughts.format.{ Uci, UciCharPair }
import draughts.opening._
import draughts.variant.Variant
import play.api.libs.json._
import scalaz.Validation.FlatMap._
import lidraughts.tree.{ Branch, Node }

trait AnaAny {
  def branch: Valid[Branch]
  def json(b: Branch, applyAmbiguity: Int = 0): JsObject
  def chapterId: Option[String]
  def path: String
}

case class AnaMove(
    orig: draughts.Pos,
    dest: draughts.Pos,
    variant: Variant,
    fen: String,
    path: String,
    chapterId: Option[String],
    promotion: Option[draughts.PromotableRole],
    puzzle: Option[Boolean],
    uci: Option[String]
) extends AnaAny {

  def branch: Valid[Branch] = {
    val oldGame = draughts.DraughtsGame(variant.some, fen.some)
    val captures = uci.flatMap(Uci.Move.apply).flatMap(_.capture)
    oldGame(orig, dest, promotion, draughts.MoveMetrics(), captures.isDefined, captures) flatMap {
      case (game, move) => {
        game.pdnMoves.lastOption toValid "Moved but no last move!" map { san =>
          val uci = Uci(move, captures.isDefined)
          val movable = game.situation playable false
          val fen = draughts.format.Forsyth >> game
          val destinations = if (game.situation.ghosts > 0) Map(dest -> game.situation.destinationsFrom(dest)) else game.situation.allDestinations
          val captLen = if (game.situation.ghosts > 0) game.situation.captureLengthFrom(dest) else game.situation.allMovesCaptureLength
          val alts =
            if (~puzzle && game.situation.ghosts == 0 && ~captLen > 2)
              game.situation.validMovesFinal.values.toList.flatMap(_.map { m =>
                Node.Alternative(
                  uci = m.toUci.uci,
                  fen = draughts.format.Forsyth.exportBoard(m.after).some
                )
              }).take(100).some
            else none
          Branch(
            id = UciCharPair(uci),
            ply = game.turns,
            move = Uci.WithSan(uci, san),
            fen = fen,
            dests = Some(movable ?? destinations),
            captureLength = movable ?? captLen,
            opening = (game.turns <= 30 && Variant.openingSensibleVariants(variant)) ?? {
              FullOpeningDB findByFen fen
            },
            drops = if (movable) game.situation.drops else Some(Nil),
            alternatives = alts
          )
        }
      }
    }
  }

  def json(b: Branch, applyAmbiguity: Int = 0): JsObject = Json.obj(
    "node" -> (if (applyAmbiguity != 0) b.copy(id = UciCharPair(b.id.a, applyAmbiguity)) else b),
    "path" -> path
  ).add("ch" -> chapterId)
}

object AnaMove {

  def parse(o: JsObject) = for {
    d ← o obj "d"
    orig ← d str "orig" flatMap draughts.Pos.posAt
    dest ← d str "dest" flatMap draughts.Pos.posAt
    fen ← d str "fen"
    path ← d str "path"
  } yield AnaMove(
    orig = orig,
    dest = dest,
    variant = draughts.variant.Variant orDefault ~d.str("variant"),
    fen = fen,
    path = path,
    chapterId = d str "ch",
    promotion = d str "promotion" flatMap draughts.Role.promotable,
    puzzle = d boolean "puzzle",
    uci = d str "uci"
  )
}
