package com.nulabinc.backlog.migration.common.dsl

import java.nio.file.Path

trait StorageDSL[F[_]] {

  def readFile(path: Path): F[String]

  def writeFile(path: Path, content: String): F[Unit]

  def exists(path: Path): F[Boolean]

  def delete(path: Path): F[Boolean]

}
