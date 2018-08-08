package lidraughts.common

trait Cli {

  def process: PartialFunction[List[String], Fu[String]]

}
