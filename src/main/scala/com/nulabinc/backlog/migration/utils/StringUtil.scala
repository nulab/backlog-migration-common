package com.nulabinc.backlog.migration.utils

/**
  * @author uchida
  */
object StringUtil {

  def safeStringToInt(str: String): Option[Int] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toInt
  }

}
