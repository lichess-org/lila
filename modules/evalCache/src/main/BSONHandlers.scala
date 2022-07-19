package lila.evalCache

import reactivemongo.api.bson._
import scala.util.{ Success, Try }
import cats.data.NonEmptyList

import shogi.format.usi.Usi
import lila.db.dsl._
import lila.tree.Eval._

private object BSONHandlers {

  import EvalCacheEntry._

  implicit private val TrustBSONHandler  = doubleAnyValHandler[Trust](_.value, Trust.apply)
  implicit private val KnodesBSONHandler = intAnyValHandler[Knodes](_.value, Knodes.apply)

  implicit val PvsHandler = new BSONHandler[NonEmptyList[Pv]] {
    private def scoreWrite(s: Score): String = s.value.fold(_.value.toString, m => s"#${m.value}")
    private def scoreRead(str: String): Option[Score] =
      if (str startsWith "#") str.drop(1).toIntOption map { m =>
        Score mate Mate(m)
      }
      else
        str.toIntOption map { c =>
          Score cp Cp(c)
        }
    private def movesWrite(moves: Moves): String = moves.value.toList.map(_.usi) mkString " "
    private def movesRead(str: String): Option[Moves] =
      Usi readList str flatMap (_.toNel) map Moves.apply
    private val scoreSeparator = ':'
    private val pvSeparator    = '/'
    private val pvSeparatorStr = pvSeparator.toString
    def readTry(bs: BSONValue) =
      bs match {
        case BSONString(value) =>
          Try {
            value.split(pvSeparator).toList.map { pvStr =>
              pvStr.split(scoreSeparator) match {
                case Array(score, moves) =>
                  Pv(
                    scoreRead(score) err s"Invalid score $score",
                    movesRead(moves) err s"Invalid moves $moves"
                  )
                case x => sys error s"Invalid PV $pvStr: ${x.toList} (in ${value})"
              }
            }
          }.flatMap {
            _.toNel toTry s"Empty PVs ${value}"
          }
        case b => lila.db.BSON.handlerBadType[NonEmptyList[Pv]](b)
      }
    def writeTry(x: NonEmptyList[Pv]) =
      Success(BSONString {
        x.toList.map { pv =>
          s"${scoreWrite(pv.score)}$scoreSeparator${movesWrite(pv.moves)}"
        } mkString pvSeparatorStr
      })
  }

  implicit val EntryIdHandler = tryHandler[Id](
    { case BSONString(value) =>
      value split ':' match {
        case Array(sfen) => Success(Id(shogi.variant.Standard, SmallSfen raw sfen))
        case Array(variantId, sfen) =>
          Success(
            Id(
              shogi.variant.Variant.orDefault(~variantId.toIntOption),
              SmallSfen raw sfen
            )
          )
        case _ => lila.db.BSON.handlerBadValue(s"Invalid evalcache id ${value}")
      }
    },
    x =>
      BSONString {
        if (x.variant.standard) x.smallSfen.value
        else s"${x.variant.id}:${x.smallSfen.value}"
      }
  )

  implicit val evalHandler  = Macros.handler[Eval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
