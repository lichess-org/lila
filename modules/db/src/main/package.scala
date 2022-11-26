package lila.db

import reactivemongo.api.commands.WriteResult

export lila.Lila.{ *, given }

def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] =
  case wr: WriteResult if isDuplicateKey(wr) => f(wr)

def ignoreDuplicateKey: PartialFunction[Throwable, Unit] =
  case wr: WriteResult if isDuplicateKey(wr) => ()

def isDuplicateKey(wr: WriteResult) = wr.code.contains(11000)

private val logger = lila.log("db")
