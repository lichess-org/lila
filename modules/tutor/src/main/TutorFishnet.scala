package lila.tutor

import scala.concurrent.ExecutionContext

import lila.analyse.AnalysisRepo
import lila.fishnet.Analyser
import lila.fishnet.FishnetAwaiter
import lila.game.GameRepo
import lila.user.User

final private class TutorFishnet(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    analyser: Analyser,
    awaiter: FishnetAwaiter
)(implicit
    ec: ExecutionContext
) {

  def ensureSomeAnalysis(user: User): Funit = ???

  // private def getAnalysis(userId: User.ID, ip: IpAddress, game: Game, index: Int) =
  //   analysisRepo.byGame(game) orElse {
  //     (index < requireAnalysisOnLastGames) ?? requestAnalysis(
  //       game,
  //       lila.fishnet.Work.Sender(userId = userId, ip = ip.some, mod = false, system = false)
  //     )
  //   }

  // private def requestAnalysis(game: Game, sender: lila.fishnet.Work.Sender): Fu[Option[Analysis]] = {
  //   def fetch = analysisRepo byId game.id
  //   fishnetAnalyser(game, sender, ignoreConcurrentCheck = true) flatMap {
  //     case Analyser.Result.Ok              => fishnetAwaiter(game.id, timeToWaitForAnalysis) >> fetch
  //     case Analyser.Result.AlreadyAnalysed => fetch
  //     case _                               => fuccess(none)
  //   }
  // }
}
