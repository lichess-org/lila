package lila.report

import lila.user.{ Title, User }

final private class ReportScore(
    getAccuracy: ReporterId => Fu[Option[Accuracy]]
) {

  def apply(candidate: Report.Candidate): Fu[Report.Candidate.Scored] =
    getAccuracy(candidate.reporter.id) map { accuracy =>
      impl.baseScore +
        impl.accuracyScore(accuracy) +
        impl.reporterScore(candidate.reporter) +
        impl.autoScore(candidate)
    } map
      impl.fixedAutoCommPrintScore(candidate) map
      impl.commFlagScore(candidate) map { score =>
      candidate scored Report.Score(score atLeast 5 atMost 100)
    }

  private object impl {

    val baseScore               = 30
    val baseScoreAboveThreshold = 50

    def accuracyScore(a: Option[Accuracy]): Double = a ?? { accuracy =>
      (accuracy.value - 50) * 0.7d
    }

    def reporterScore(r: Reporter) =
      titleScore(r.user.title) + flagScore(r.user)

    def titleScore(title: Option[Title]) =
      title.isDefined ?? 30d

    def flagScore(user: User) =
      user.lameOrTroll ?? -30d

    def autoScore(candidate: Report.Candidate) =
      candidate.isAutomatic ?? 20d

    // https://github.com/ornicar/lila/issues/4093
    // https://github.com/ornicar/lila/issues/4587
    def fixedAutoCommPrintScore(c: Report.Candidate)(score: Double): Double =
      if (c.isAutoComm) baseScore
      else if (c.isPrint || c.isCoachReview || c.isPlaybans) baseScoreAboveThreshold
      else score

    def commFlagScore(c: Report.Candidate)(score: Double): Double =
      if (c.isCommFlag) score / 2
      else score
  }
}
