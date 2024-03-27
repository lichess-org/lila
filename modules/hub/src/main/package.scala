package lila.hub

export lila.Lila.{ *, given }

type InfoAdvice  = (Info, Option[Advice])
type InfoAdvices = List[InfoAdvice]
