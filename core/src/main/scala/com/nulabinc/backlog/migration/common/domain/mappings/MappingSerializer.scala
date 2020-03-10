package com.nulabinc.backlog.migration.common.domain.mappings

import java.nio.charset.{Charset, StandardCharsets}

import monix.reactive.Observable

object MappingSerializer {

  private val charset: Charset = StandardCharsets.UTF_8

  def status[A](mappings: Seq[StatusMapping[A]])
               (implicit serializer: Serializer[StatusMapping[A], Seq[String]]): Observable[Array[Byte]] =
    toObservable(mappings)

  def priority[A](mappings: Seq[PriorityMapping[A]])
                 (implicit serializer: Serializer[PriorityMapping[A], Seq[String]]): Observable[Array[Byte]] =
    toObservable(mappings)

  def user[A](mappings: Seq[UserMapping[A]])
             (implicit serializer: Serializer[UserMapping[A], Seq[String]]): Observable[Array[Byte]] =
    toObservable(mappings)

  private def toObservable[A](mappings: Seq[A])(implicit serializer: Serializer[A, Seq[String]]): Observable[Array[Byte]] =
    Observable
      .fromIteratorUnsafe(mappings.iterator)
      .map(serializer.serialize)
      .map(toRow)
      .map(toByteArray)

  private def toRow(values: Seq[String]): String =
    s"""${values.map(s => "\"" + s + "\"" ).mkString(", ")}\n""".stripMargin

  private def toByteArray(str: String): Array[Byte] =
    str.getBytes(charset)
}
