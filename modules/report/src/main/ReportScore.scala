package lila.report

final private class ReportScore(
    getAccuracy: ReporterId => Fu[Option[Accuracy]]
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(candidate: Report.Candidate): Fu[Report.Candidate.Scored] =
    getAccuracy(candidate.reporter.id) map { accuracy =>
      impl.baseScore +
        impl.accuracyScore(accuracy) +
        impl.reporterScore(candidate.reporter) +
        impl.autoScore(candidate)
    } map
      impl.fixedAutoScore(candidate) map { score =>
        candidate scored Report.Score(score atLeast 5 atMost 100)
      }

  private object impl {

    val baseScore = 20

    def accuracyScore(a: Option[Accuracy]): Double =
      a ?? { accuracy =>
        (accuracy.value - 50) * 0.8d
      }

    def reporterScore(r: Reporter) = r.user.lameOrTroll ?? -30d

    def autoScore(candidate: Report.Candidate) = candidate.isAutomatic ?? 25d

    // https://github.com/lichess-org/lila/issues/4587
    def fixedAutoScore(c: Report.Candidate)(score: Double): Double =
      if (c.isAutoBoost) baseScore * 1.5
      else if (c.isAutoComm) 42d
      else if (c.isIrwinCheat) 60d
      else if (c.isPrint || c.isCoachReview || c.isPlaybans) baseScore * 2
      else score
  }
}
