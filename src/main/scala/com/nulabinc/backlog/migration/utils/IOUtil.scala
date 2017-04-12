package com.nulabinc.backlog.migration.utils

import scalax.file.Path

/**
  * @author uchida
  */
object IOUtil {

  def createDirectory(path: Path): Unit = {
    if (!path.isDirectory) path.createDirectory()
  }

  def input(path: Path): Option[String] = {
    if (path.isFile) Some(path.lines().mkString)
    else None
  }

  def output(path: Path, content: String) = {
    if (!path.isFile) path.createFile()
    path.write(content)
  }

  def directoryPaths(path: Path): Seq[Path] = {
    if (path.isDirectory)
      path.toAbsolute.children().filter(_.isDirectory).toSeq
    else Seq.empty[Path]
  }

  def isDirectory(path: String): Boolean = {
    val filePath: Path = Path.fromString(path).toAbsolute
    filePath.isDirectory
  }

  def rename(from: Path, to: Path) =
    from.toAbsolute.moveTo(to.toAbsolute)

}
