package lila.evaluation

sealed trait AccountAction:
  val description: String
  val id: Int
  override def toString = description

object AccountAction:
  case object EngineAndBan extends AccountAction:
    val description: String = "Mark as engine"
    val id = 4
  case object Engine extends AccountAction:
    val description: String = "Mark as engine"
    val id = 3
  case class Report(reason: String) extends AccountAction:
    val description: String = s"Report for $reason"
    val id = 2
  val reportVariousReasons = Report("")
  val reportConsistentMovetimes = Report("consistent movetimes")
  case object Nothing extends AccountAction:
    val description: String = "Not suspicious"
    val id = 1
