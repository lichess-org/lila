package lila.puzzle

import scala.concurrent.ExecutionContext
import lila.user.User

case class PuzzleDashboard(
)

final class PuzzleDashboardApi(
    colls: PuzzleColls
)(implicit ec: ExecutionContext) {

  def apply(u: User): Fu[PuzzleDashboard] = ???
}
