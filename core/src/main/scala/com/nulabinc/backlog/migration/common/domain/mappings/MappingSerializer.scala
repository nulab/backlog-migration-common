package com.nulabinc.backlog.migration.common.domain.mappings

import java.nio.charset.{Charset, StandardCharsets}

import monix.reactive.Observable

trait Serializer[A, B] {
  def serialize(a: A): B
}

trait MappingSerializer[A] {

  private val charset: Charset = StandardCharsets.UTF_8

  private implicit class StringByteOps(str: String) {
    def toByteArray: Array[Byte] = str.getBytes(charset)
  }

  val statusSerializer: Serializer[StatusMapping[A], String]

  def status(mappings: Seq[StatusMapping[A]]): Observable[Array[Byte]] =
    Observable
      .fromIteratorUnsafe(mappings.iterator)
      .map(statusSerializer.serialize)
      .map(_.toByteArray)
}
