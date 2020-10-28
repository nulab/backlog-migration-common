package com.nulabinc.backlog.migration.common.dsl

import java.io.InputStream
import java.nio.file.Path

import monix.reactive.Observable
import simulacrum.typeclass

@typeclass
trait StorageDSL[F[_]] {

  def read[A](path: Path, f: InputStream => A): F[A]

  def writeFile(path: Path, content: String): F[Unit]

  def writeNewFile(path: Path, stream: Observable[Array[Byte]]): F[Unit]

  def writeAppendFile(path: Path, stream: Observable[Array[Byte]]): F[Unit]

  def exists(path: Path): F[Boolean]

  def delete(path: Path): F[Unit]

}
