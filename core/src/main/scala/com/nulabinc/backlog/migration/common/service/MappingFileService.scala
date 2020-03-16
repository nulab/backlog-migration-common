package com.nulabinc.backlog.migration.common.service

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}

import monix.reactive.Observable
import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}

import scala.jdk.CollectionConverters._

object MappingFileService {

  private val charset: Charset = StandardCharsets.UTF_8
  private val csvFormat: CSVFormat = CSVFormat.DEFAULT.withIgnoreEmptyLines().withSkipHeaderRecord()

  def readLine(is: InputStream): IndexedSeq[CSVRecord] =
    CSVParser.parse(is, charset, csvFormat)
      .getRecords.asScala
      .foldLeft(IndexedSeq.empty[CSVRecord])( (acc, item) => acc :+ item)
      .tail // drop header

  def readLineStream(is: InputStream): Observable[CSVRecord] =
    Observable
      .fromIteratorUnsafe(CSVParser.parse(is, charset, csvFormat).iterator().asScala)
      .drop(1)

}
