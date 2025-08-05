package lila.core
package shutup

import lila.core.id.*
import lila.core.userId.UserId

enum PublicSource(val typeName: String):
  case Tournament(id: TourId) extends PublicSource("tournament")
  case Simul(id: SimulId) extends PublicSource("simul")
  case Study(id: StudyId) extends PublicSource("study")
  case Watcher(gameId: GameId) extends PublicSource("watcher")
  case Team(id: TeamId) extends PublicSource("team")
  case Swiss(id: SwissId) extends PublicSource("swiss")
  case Forum(id: ForumPostId) extends PublicSource("forum")
  case Ublog(id: UblogPostId) extends PublicSource("ublog")
  case Relay(id: RelayRoundId) extends PublicSource("relay")

object PublicSource:
  object longNotation:
    def read(str: String): Option[PublicSource] = str.split('/') match
      case Array("tournament", id) => Some(PublicSource.Tournament(TourId(id)))
      case Array("simul", id) => Some(PublicSource.Simul(SimulId(id)))
      case Array("game", id) => Some(PublicSource.Watcher(GameId(id)))
      case Array("study", id) => Some(PublicSource.Study(StudyId(id)))
      case Array("team", id) => Some(PublicSource.Team(TeamId(id)))
      case Array("swiss", id) => Some(PublicSource.Swiss(SwissId(id)))
      case Array("forum", id) => Some(PublicSource.Forum(ForumPostId(id)))
      case Array("blog", id) => Some(PublicSource.Ublog(UblogPostId(id)))
      case Array("relay", id) => Some(PublicSource.Relay(RelayRoundId(id)))
      case _ => None
    def write(s: PublicSource): String = s match
      case PublicSource.Tournament(id) => s"tournament/$id"
      case PublicSource.Simul(id) => s"simul/$id"
      case PublicSource.Watcher(gameId) => s"game/$gameId"
      case PublicSource.Study(id) => s"study/$id"
      case PublicSource.Team(id) => s"team/$id"
      case PublicSource.Swiss(id) => s"swiss/$id"
      case PublicSource.Forum(id) => s"forum/$id"
      case PublicSource.Ublog(id) => s"blog/$id"
      case PublicSource.Relay(id) => s"relay/$id"

case class PublicLine(text: String, from: PublicSource, date: Instant)

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
