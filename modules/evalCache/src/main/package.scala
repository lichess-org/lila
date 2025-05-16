package lila.evalCache

export lila.core.lilaism.Lilaism.{ *, given }
import lila.tree.CloudEval
import lila.core.chess.MultiPv

extension (e: CloudEval)
  def multiPv = MultiPv(e.pvs.size)
  def takePvs(multiPv: MultiPv) =
    e.copy(pvs = NonEmptyList(e.pvs.head, e.pvs.tail.take(multiPv.value - 1)))
