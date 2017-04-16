package com.nulabinc.backlog.migration.utils

/**
  * @author uchida
  */
object StringUtil {

  def safeStringToInt(str: String): Option[Int] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toInt
  }

  def safeStringToLong(str: String): Option[Long] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toLong
  }

  def safeEquals(value: Int, string: String): Boolean =
    safeStringToInt(string) match {
      case Some(intValue) => intValue == value
      case _              => false
    }

}
