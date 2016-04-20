package lila

import lila.socket.WithSocket

package object study extends PackageObject with WithPlay with WithSocket {

  private[study] val logger = lila.log("study")

  private[study]type MemberMap = Map[lila.user.User.ID, lila.study.StudyMember]

  private[study]type ChapterMap = Map[lila.study.Chapter.ID, lila.study.Chapter]
}
