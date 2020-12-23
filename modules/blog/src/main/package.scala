package lila

package object blog extends PackageObject {

  private[blog] def logger = lila.log("blog")

  lazy val thisYear = org.joda.time.DateTime.now.getYear

  lazy val allYears = (thisYear to 2020 by -1).toList
}
