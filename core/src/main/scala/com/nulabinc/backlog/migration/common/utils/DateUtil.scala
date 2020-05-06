package com.nulabinc.backlog.migration.common.utils

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

/**
  * @author uchida
  */
object DateUtil {

  private[this] val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
  private[this] val DATE_FORMAT = "yyyy-MM-dd"
  private[this] val SLASH_DATE_FORMAT = "yyyy/MM/dd"
  private[this] val YYYYMMDD_FORMAT = "yyyyMMdd"
  private[this] val TIME_FORMAT = "HH:mm:ss"

  private[this] val ISO = new SimpleDateFormat(ISO_FORMAT)
  private[this] val DATE = new SimpleDateFormat(DATE_FORMAT)
  private[this] val SLASH = new SimpleDateFormat(SLASH_DATE_FORMAT)
  private[this] val YYYYMMDD = new SimpleDateFormat(YYYYMMDD_FORMAT)
  private[this] val TIME = new SimpleDateFormat(TIME_FORMAT)
  TIME.setTimeZone(TimeZone.getTimeZone("UTC"))

  def slashFormat(date: Date): String = {
    synchronized {
      Option(date).map(SLASH.format).getOrElse("")
    }
  }

  def dateFormat(date: Date): String = {
    synchronized {
      Option(date).map(DATE.format).getOrElse("")
    }
  }

  def isoFormat(date: Date): String = {
    synchronized {
      Option(date).map(ISO.format).getOrElse("")
    }
  }

  def yyyymmddFormat(date: Date): String = {
    synchronized {
      Option(date).map(YYYYMMDD.format).getOrElse("")
    }
  }

  def timeFormat(date: Date): String = {
    synchronized {
      Option(date).map(TIME.format).getOrElse("")
    }
  }

  def yyyymmddParse(value: String): Date = {
    synchronized {
      YYYYMMDD.parse(value)
    }
  }

  def isoParse(value: String): Date = {
    synchronized {
      ISO.parse(value)
    }
  }

  def slashDateParse(value: String): Date = {
    synchronized {
      SLASH.parse(value)
    }
  }

  def tryIsoParse(optDate: Option[String]): Date = {
    optDate match {
      case Some(date) => isoParse(date)
      case None       => throw new RuntimeException("Date value not found.")
    }
  }

  def isoToDateFormat(value: String): String = {
    dateFormat(isoParse(value))
  }

  def yyyymmddToDateFormat(value: String): String = {
    dateFormat(yyyymmddParse(value))
  }

  def yyyymmddToSlashFormat(value: String): String = {
    slashFormat(yyyymmddParse(value))
  }

  def slashDateToDate(value: String): String = {
    dateFormat(slashDateParse(value))
  }

  def formatIfNeeded(value: String) = {
    val pattern = """\d{4}/\d{2}/\d{2}""".r
    value match {
      case pattern() => dateFormat(slashDateParse(value))
      case _         => value
    }
  }

}
