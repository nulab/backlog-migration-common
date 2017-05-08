package com.nulabinc.backlog.migration.common.utils

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

  def notEmpty(s: String): Option[String] = {
    Option(s) match {
      case Some(string) if (string.trim.nonEmpty) => Some(string)
      case _                                      => None
    }
  }

}
