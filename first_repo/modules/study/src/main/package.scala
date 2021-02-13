package lila

import reactivemongo.api.ReadPreference

package object study extends PackageObject {

  private[study] val logger = lila.log("study")

  private[study] type ChapterMap = Map[lila.study.Chapter.Id, lila.study.Chapter]

  private[study] val readPref = ReadPreference.primary
}
