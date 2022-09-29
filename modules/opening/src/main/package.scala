package lila

package object opening extends PackageObject {

  private[opening] val logger = lila.log("opening")

  type OpeningHistory[T] = List[OpeningHistorySegment[T]]
}
