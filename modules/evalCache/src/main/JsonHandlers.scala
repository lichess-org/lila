package lidraughts.evalCache

import play.api.libs.json._

import draughts.format.{ Uci, FEN }
import EvalCacheEntry._
import lidraughts.common.PimpedJson._
import lidraughts.tree.Eval._

object JsonHandlers {

  private implicit val cpWriter = intAnyValWriter[Cp](_.value)
  private implicit val winWriter = intAnyValWriter[Win](_.value)
  private implicit val knodesWriter = intAnyValWriter[Knodes](_.value)

  def writeEval(e: Eval, fen: FEN) = Json.obj(
    "fen" -> fen.value,
    "knodes" -> e.knodes,
    "depth" -> e.depth,
    "pvs" -> e.pvs.toList.map(writePv)
  )

  private def writePv(pv: Pv) = Json.obj(
    "moves" -> pv.moves.value.toList.map(_.uci).mkString(" ")
  )
    .add("cp", pv.score.cp)
    .add("win", pv.score.win)

  def readPut(trustedUser: TrustedUser, o: JsObject): Option[Input.Candidate] = for {
    d <- o obj "d"
    variant = draughts.variant.Variant orDefault ~d.str("variant")
    fen <- d str "fen"
    knodes <- d int "knodes"
    depth <- d int "depth"
    pvObjs <- d objs "pvs"
    pvs <- pvObjs.map(parsePv).sequence.flatMap(_.toNel)
  } yield Input.Candidate(variant, fen, Eval(
    pvs = pvs,
    knodes = Knodes(knodes),
    depth = depth,
    by = trustedUser.user.id,
    trust = trustedUser.trust
  ))

  private def parsePv(d: JsObject): Option[Pv] = for {
    movesStr <- d str "moves"
    moves <- movesStr.split(' ').take(EvalCacheEntry.MAX_PV_SIZE).foldLeft(List.empty[Uci].some) {
      case (Some(ucis), str) => Uci(str) map (_ :: ucis)
      case _ => None
    }.flatMap(_.reverse.toNel) map Moves.apply
    cp = d int "cp" map Cp.apply
    win = d int "win" map Win.apply
    score <- cp.map(Score.cp) orElse win.map(Score.win)
  } yield Pv(score, moves)
}
