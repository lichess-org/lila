package lila.core
package shutup

import lila.core.id.*
import lila.core.userId.UserId

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

trait TextAnalysis:
  val text: String
  val badWords: List[String]
  def dirty: Boolean
  def critical: Boolean

trait TextAnalyser:
  def apply(raw: String): TextAnalysis
  def containsLink(raw: String): Boolean
  def isCritical(raw: String): Boolean
