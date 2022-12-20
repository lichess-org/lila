package lila.evalCache

import cats.implicits._
import play.api.libs.json._

import shogi.format.forsyth.Sfen
import shogi.format.usi.{ UciToUsi, Usi }
import lila.common.Json._
import lila.evalCache.EvalCacheEntry._
import lila.tree.Eval._

object JsonHandlers {

  implicit private val cpWriter     = intAnyValWriter[Cp](_.value)
  implicit private val mateWriter   = intAnyValWriter[Mate](_.value)
  implicit private val knodesWriter = intAnyValWriter[Knodes](_.value)

  def writeEval(e: Eval, sfen: Sfen) =
    Json.obj(
      "sfen"   -> sfen,
      "knodes" -> e.knodes,
      "depth"  -> e.depth,
      "pvs"    -> e.pvs.toList.map(writePv)
    )

  private def writePv(pv: Pv) =
    Json
      .obj(
        "moves" -> pv.moves.value.toList.map(_.usi).mkString(" ")
      )
      .add("cp", pv.score.cp)
      .add("mate", pv.score.mate)

  private[evalCache] def readPut(trustedUser: TrustedUser, o: JsObject): Option[Input.Candidate] =
    o obj "d" flatMap { readPutData(trustedUser, _) }

  private[evalCache] def readPutData(trustedUser: TrustedUser, d: JsObject): Option[Input.Candidate] =
    for {
      sfen   <- d str "sfen"
      knodes <- d int "knodes"
      depth  <- d int "depth"
      pvObjs <- d objs "pvs"
      pvs    <- pvObjs.map(parsePv).sequence.flatMap(_.toNel)
      variant = shogi.variant.Variant orDefault ~d.str("variant")
    } yield Input.Candidate(
      variant,
      sfen,
      Eval(
        pvs = pvs,
        knodes = Knodes(knodes),
        depth = depth,
        by = trustedUser.user.id,
        trust = trustedUser.trust
      )
    )

  private def parsePv(d: JsObject): Option[Pv] =
    for {
      movesStr <- d str "moves"
      moves <-
        movesStr
          .split(' ')
          .take(EvalCacheEntry.MAX_PV_SIZE)
          .foldLeft(List.empty[Usi].some) {
            case (Some(usis), str) => Usi(str).orElse(UciToUsi(str)) map (_ :: usis)
            case _                 => None
          }
          .flatMap(_.reverse.toNel) map Moves.apply
      cp   = d int "cp" map Cp.apply
      mate = d int "mate" map Mate.apply
      score <- cp.map(Score.cp) orElse mate.map(Score.mate)
    } yield Pv(score, moves)
}
