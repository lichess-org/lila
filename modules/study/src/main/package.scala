package lidraughts

import lidraughts.socket.WithSocket
import lidraughts.hub.TrouperMap

package object study extends PackageObject with WithSocket {

  type SocketMap = TrouperMap[StudySocket]

  private[study] val logger = lidraughts.log("study")

  private[study] type ChapterMap = Map[lidraughts.study.Chapter.Id, lidraughts.study.Chapter]

  private[study] type LightStudyCache = lidraughts.memo.AsyncCache[lidraughts.study.Study.Id, Option[lidraughts.study.Study.LightStudy]]
}
