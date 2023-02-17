package com.nulabinc.backlog.migration.common.utils

import scala.util.matching.Regex

/**
 * @author
 *   uchida
 */
object StringUtil {

  private[this] val EOI = '\uFFFF'

  private[this] val Emoji: String = "[^\u0000-\uFFFF]"

  def safeStringToInt(str: String): Option[Int] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toInt
  }

  def safeStringToLong(str: String): Option[Long] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toLong
  }

  def safeUnitStringToBigDecimal(str: String): BigDecimal = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt BigDecimal(str) match {
      case Some(value) => value
      case _ =>
        val pattern: Regex   = """^(\d+).*$""".r
        val pattern(matched) = str
        BigDecimal(matched)
    }
  }

  def safeEquals(value: Int, string: String): Boolean =
    safeStringToInt(string) match {
      case Some(intValue) => intValue == value
      case _              => false
    }

  def notEmpty(s: String): Option[String] = {
    Option(s) match {
      case Some(string) if string.trim.nonEmpty => Some(string)
      case _                                    => None
    }
  }

  def toSafeString(str: String): String = {
    val newString = str.filter((c: Char) => c != EOI)
    newString.replaceAll(Emoji, "")
  }

}
