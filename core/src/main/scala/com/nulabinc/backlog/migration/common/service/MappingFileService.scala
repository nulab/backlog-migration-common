package com.nulabinc.backlog.migration.common.service

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}

import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}

import scala.jdk.CollectionConverters._

object MappingFileService {

  private val charset: Charset = StandardCharsets.UTF_8
  private val csvFormat: CSVFormat = CSVFormat.DEFAULT.withIgnoreEmptyLines().withSkipHeaderRecord()

  def readLine(is: InputStream): IndexedSeq[CSVRecord] =
    CSVParser.parse(is, charset, csvFormat)
      .getRecords.asScala
      .foldLeft(IndexedSeq.empty[CSVRecord])( (acc, item) => acc :+ item)

  //  private def readCSVFile(is: InputStream): HashMap[String, String] = {
  //    val parser = CSVParser.parse(is, charset, Config.csvFormat)
  //    parser.getRecords.asScala.foldLeft(HashMap.empty[String, String]) {
  //      case (acc, record) =>
  //        acc + (record.get(0) -> record.get(1))
  //    }
  //  }

}
