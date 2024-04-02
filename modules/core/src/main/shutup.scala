package lila.core
package shutup

enum PublicSource(val parentName: String):
  case Tournament(id: TourId)  extends PublicSource("tournament")
  case Simul(id: SimulId)      extends PublicSource("simul")
  case Study(id: StudyId)      extends PublicSource("study")
  case Watcher(gameId: GameId) extends PublicSource("watcher")
  case Team(id: TeamId)        extends PublicSource("team")
  case Swiss(id: SwissId)      extends PublicSource("swiss")
  case Forum(id: ForumPostId)  extends PublicSource("forum")
  case Ublog(id: UblogPostId)  extends PublicSource("ublog")

trait ShutupApi:
  def publicText(userId: UserId, text: String, source: PublicSource): Funit
  def privateMessage(userId: UserId, toUserId: UserId, text: String): Funit
  def privateChat(chatId: String, userId: UserId, text: String): Funit
  def teamForumMessage(userId: UserId, text: String): Funit
