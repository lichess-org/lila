package lila.mod

final class boostingDetector(
	api: boostingApi
	) {

	context.system.lilaBus.subscribe(self, 'finishGame)

	def receive = {
		case FinishGame(game, _, _) => api finishGame game
	}
}