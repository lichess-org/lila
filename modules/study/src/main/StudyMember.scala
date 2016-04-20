package lila.study

case class StudyMember(
  owner: Boolean,
  chapterId: Chapter.ID,
  path: Path)
