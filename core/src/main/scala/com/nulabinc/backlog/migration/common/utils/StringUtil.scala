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

  def safeUnitStringToFloat(str: String): Float = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toFloat match {
      case Some(float) => float
      case _ =>
        val pattern: Regex = """^(\d+).*$""".r
        val pattern(float) = str
        float.toFloat
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
