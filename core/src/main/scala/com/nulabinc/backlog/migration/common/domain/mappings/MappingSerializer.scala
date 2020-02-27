package com.nulabinc.backlog.migration.common.domain.mappings

import java.nio.charset.{Charset, StandardCharsets}

import monix.reactive.Observable

trait Serializer[A, B] {
  def serialize(a: A): B
}

object MappingSerializer {

  private val charset: Charset = StandardCharsets.UTF_8

  def status[A](mappings: Seq[StatusMapping[A]])
               (implicit serializer: Serializer[StatusMapping[A], Seq[String]]): Observable[Array[Byte]] =
    Observable
      .fromIteratorUnsafe(mappings.iterator)
      .map(serializer.serialize)
      .map(toRow)
      .map(toByteArray)

  private def toRow(values: Seq[String]): String =
    s""""${values.mkString(", ")}"\n""".stripMargin

  private def toByteArray(str: String): Array[Byte] =
    str.getBytes(charset)
}
