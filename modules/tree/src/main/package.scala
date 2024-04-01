package lila.tree

export lila.Common.{ *, given }

type InfoAdvice  = (Info, Option[Advice])
type InfoAdvices = List[InfoAdvice]
