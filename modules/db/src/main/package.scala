package lila.db

import reactivemongo.api.commands.WriteResult

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

trait NoBSONWriter[A] // don't create default BSONWiter for this type
trait NoBSONReader[A] // don't create default BSONReader for this type
trait NoDbHandler[A] extends NoBSONWriter[A] with NoBSONReader[A]

def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] =
  case wr: WriteResult if isDuplicateKey(wr) => f(wr)

def ignoreDuplicateKey: PartialFunction[Throwable, Unit] =
  case wr: WriteResult if isDuplicateKey(wr) => ()

def isDuplicateKey(wr: WriteResult) = wr.code.contains(11000)

type AggregationPipeline[Operator] = (Operator, List[Operator])

private val logger = lila.log("db")
