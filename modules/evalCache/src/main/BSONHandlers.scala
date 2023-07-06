package lila.evalCache

import reactivemongo.api.bson.*
import scala.util.{ Success, Try }

import chess.format.Uci
import lila.db.dsl.{ *, given }
import lila.tree.Score

private object BSONHandlers:

  import EvalCacheEntry.*

  given BSONReader[NonEmptyList[Pv]] = new:

    private def scoreRead(str: String): Option[Score] =
      if str startsWith "#" then str.drop(1).toIntOption.map(Score.mate)
      else str.toIntOption.map(Score.cp)

    private def movesRead(str: String): Option[Moves] = Moves from:
      Uci readListChars str flatMap (_.toNel)

    private val scoreSeparator = ':'
    private val pvSeparator    = '/'
    def readTry(bs: BSONValue) =
      bs match
        case BSONString(value) =>
          Try {
            value.split(pvSeparator).toList.map { pvStr =>
              pvStr.split(scoreSeparator) match
                case Array(score, moves) =>
                  Pv(
                    scoreRead(score) err s"Invalid score $score",
                    movesRead(moves) err s"Invalid moves $moves"
                  )
                case x => sys error s"Invalid PV $pvStr: ${x.toList} (in $value)"
            }
          }.flatMap {
            _.toNel toTry s"Empty PVs $value"
          }
        case b => lila.db.BSON.handlerBadType[NonEmptyList[Pv]](b)

  given BSONHandler[Id] = tryHandler[Id](
    { case BSONString(value) =>
      value split ':' match
        case Array(fen) => Success(Id(chess.variant.Standard, SmallFen(fen)))
        case Array(variantId, fen) =>
          import chess.variant.Variant
          Success(
            Id(
              Variant.Id.from(variantId.toIntOption) flatMap {
                Variant(_)
              } err s"Invalid evalcache variant $variantId",
              SmallFen(fen)
            )
          )
        case _ => lila.db.BSON.handlerBadValue(s"Invalid evalcache id $value")
    },
    x =>
      BSONString {
        if x.variant.standard || x.variant.fromPosition then x.smallFen.value
        else s"${x.variant.id}:${x.smallFen.value}"
      }
  )

  given BSONDocumentReader[Eval]           = Macros.reader
  given BSONDocumentReader[EvalCacheEntry] = Macros.reader
