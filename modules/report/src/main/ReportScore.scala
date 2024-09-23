package lila.report

final private class ReportScore(
    getAccuracy: Report.Candidate => Fu[Option[Accuracy]],
    gameRepo: lila.core.game.GameRepo
)(using domain: lila.core.config.NetDomain)(using Executor):

  def apply(candidate: Report.Candidate): Fu[Report.Candidate.Scored] =
    getAccuracy(candidate)
      .map: accuracy =>
        impl.baseScore +
          impl.accuracyScore(accuracy) +
          impl.reporterScore(candidate.reporter) +
          impl.autoScore(candidate)
      .map(impl.fixedAutoScore(candidate))
      .map(Report.Score(_))
      .map(_.withinBounds)
      .map(candidate.scored)
      .flatMap(impl.dropScoreIfCheatReportHasNoAnalyzedGames)
      .map(_.withScore(_.withinBounds))

  private object impl:

    val baseScore = 20

    def accuracyScore(a: Option[Accuracy]): Double =
      a.so: accuracy =>
        (accuracy.value - 50) * 0.8d

    def reporterScore(r: Reporter) = r.user.lameOrTroll.so(-30d)

    def autoScore(candidate: Report.Candidate) = candidate.isAutomatic.so(25d)

    // https://github.com/lichess-org/lila/issues/4587
    def fixedAutoScore(c: Report.Candidate)(score: Double): Double =
      import Reason.*
      if c.isAutoBoost then baseScore * 1.5
      else if c.isAutoComm then 42d
      else if c.isIrwinCheat then 45d
      else if c.isKaladinCheat then 25d
      else if c.reporter.user.marks.reportban then score // reportbanned can still make comm reports
      else if c.reason == AltPrint || c.reason == Playbans then baseScore * 2
      else if Set(Violence, Harass, SelfHarm, Hate).contains(c.reason) then 50d
      else if c.reason == Username then 50d
      else score

    def dropScoreIfCheatReportHasNoAnalyzedGames(c: Report.Candidate.Scored): Fu[Report.Candidate.Scored] =
      if c.candidate.isCheat & !c.candidate.isIrwinCheat & !c.candidate.isKaladinCheat then
        def isUsable(gameId: GameId) = gameRepo.analysed(gameId).map(_.exists(_.ply > 30))
        ReportForm
          .gameLinks(c.candidate.text)
          .existsM(isUsable)
          .map:
            if _ then c
            else c.withScore(_.map(_ / 3))
      else fuccess(c)
