package lila.evalCache

import cats.implicits.*
import chess.format.{ Fen, Uci }
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.evalCache.EvalCacheEntry.*
import lila.tree.Eval.*

object JsonHandlers:

  private given Writes[Cp]     = writeAs(_.value)
  private given Writes[Mate]   = writeAs(_.value)
  private given Writes[Knodes] = writeAs(_.value)

  def writeEval(e: Eval, fen: Fen.Epd) =
    Json.obj(
      "fen"    -> fen,
      "knodes" -> e.knodes,
      "depth"  -> e.depth,
      "pvs"    -> JsArray(e.pvs.toList.map(writePv))
    )

  private def writePv(pv: Pv) =
    Json
      .obj(
        "moves" -> pv.moves.value.toList.map(_.uci).mkString(" ")
      )
      .add("cp", pv.score.cp)
      .add("mate", pv.score.mate)

  private[evalCache] def readPut(trustedUser: TrustedUser, o: JsObject): Option[Input.Candidate] =
    o obj "d" flatMap { readPutData(trustedUser, _) }

  private[evalCache] def readPutData(trustedUser: TrustedUser, d: JsObject): Option[Input.Candidate] =
    import chess.variant.Variant
    for
      fen    <- d.get[Fen.Epd]("fen")
      knodes <- d int "knodes"
      depth  <- d int "depth"
      pvObjs <- d objs "pvs"
      pvs    <- pvObjs.map(parsePv).sequence.flatMap(_.toNel)
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
    yield Input.Candidate(
      variant,
      fen,
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
      moves <- Moves from
        movesStr
          .split(' ')
          .take(MAX_PV_SIZE)
          .foldLeft(List.empty[Uci].some) {
            case (Some(ucis), str) => Uci(str) map (_ :: ucis)
            case _                 => None
          }
          .flatMap(_.reverse.toNel)
      score <- d.get[Cp]("cp").map(Score.cp(_)) orElse d.get[Mate]("mate").map(Score.mate(_))
    } yield Pv(score, moves)
