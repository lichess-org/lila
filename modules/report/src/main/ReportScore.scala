package lila.report

import lila.user.User

private final class ReportScore(
    getAccuracy: ReporterId => Fu[Option[Accuracy]]
) {

  def apply(candidate: Report.Candidate): Fu[Report.Candidate.Scored] =
    getAccuracy(candidate.reporter.id) map { accuracy =>
      impl.accuracyScore(accuracy) + impl.reporterScore(candidate.reporter)
    } map { score =>
      candidate scored Report.Score(score atLeast 0 atMost 100)
    }

  private object impl {

    def accuracyScore(a: Option[Accuracy]): Double = a ?? { accuracy =>
      (accuracy.value - 50) * 0.7d
    }

    def reporterScore(r: Reporter) =
      titleScore(r.user.title) + flagScore(r.user)

    def titleScore(title: Option[String]) =
      (title.isDefined) ?? 30d

    def flagScore(user: User) =
      (user.lameOrTroll) ?? -30d
  }
}
