package lila.evalCache

import reactivemongo.api.bson.*
import scala.util.{ Success, Try }
import cats.data.NonEmptyList

import chess.format.Uci
import lila.db.dsl.{ *, given }
import lila.tree.Eval.*

private object BSONHandlers:

  import EvalCacheEntry.*

  given BSONHandler[NonEmptyList[Pv]] = new BSONHandler[NonEmptyList[Pv]]:
    private def scoreWrite(s: Score): String = s.value.fold(_.value.toString, m => s"#${m.value}")
    private def scoreRead(str: String): Option[Score] =
      if (str startsWith "#") str.drop(1).toIntOption map { m =>
        Score mate Mate(m)
      }
      else
        str.toIntOption map { c =>
          Score cp Cp(c)
        }
    private def movesWrite(moves: Moves): String = Uci writeListChars moves.value.toList
    private def movesRead(str: String): Option[Moves] = Moves from {
      Uci readListChars str flatMap (_.toNel)
    }
    private val scoreSeparator = ':'
    private val pvSeparator    = '/'
    private val pvSeparatorStr = pvSeparator.toString
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
    def writeTry(x: NonEmptyList[Pv]) =
      Success(BSONString {
        x.toList.map { pv =>
          s"${scoreWrite(pv.score)}$scoreSeparator${movesWrite(pv.moves)}"
        } mkString pvSeparatorStr
      })

  given BSONHandler[Id] = tryHandler[Id](
    { case BSONString(value) =>
      value split ':' match {
        case Array(fen) => Success(Id(chess.variant.Standard, SmallFen(fen)))
        case Array(variantId, fen) =>
          Success(
            Id(
              variantId.toIntOption flatMap chess.variant.Variant.apply err s"Invalid evalcache variant $variantId",
              SmallFen(fen)
            )
          )
        case _ => lila.db.BSON.handlerBadValue(s"Invalid evalcache id $value")
      }
    },
    x =>
      BSONString {
        if (x.variant.standard || x.variant.fromPosition) x.smallFen.value
        else s"${x.variant.id}:${x.smallFen.value}"
      }
  )

  given BSONDocumentHandler[Eval]           = Macros.handler
  given BSONDocumentHandler[EvalCacheEntry] = Macros.handler
