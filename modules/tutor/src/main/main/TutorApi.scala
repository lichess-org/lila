package lila.tutor

import scala.concurrent.ExecutionContext
import lila.user.User

final class TutorApi(builder: TutorBuilder)(implicit ec: ExecutionContext) {

  def latest(user: User): Fu[TutorReport.Availability] = ???
}
