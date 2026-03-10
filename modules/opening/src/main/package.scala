package lila.opening

export lila.core.lilaism.Lilaism.{ *, given }
import lila.core.lilaism.LilaExceptionNoStack
export lila.common.extensions.*

private val logger = lila.log("opening")

type PopularityHistoryAbsolute = List[Long]
type PopularityHistoryPercent = List[Float]

case class WaitOrLogin(message: String) extends LilaExceptionNoStack
