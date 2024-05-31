package lila.study
package actorApi

case class StartStudy(studyId: Study.Id)
case class SaveStudy(study: Study)
case class SetTag(chapterId: Chapter.Id, name: String, value: String) {
  def tag = shogi.format.Tag(name, value take 140)
}
case class StudyLikes(studyId: Study.Id, likes: Study.Likes)

case class Who(u: lila.user.User.ID, sri: lila.socket.Socket.Sri)
