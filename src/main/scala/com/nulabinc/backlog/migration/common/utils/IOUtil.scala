package com.nulabinc.backlog.migration.common.utils

import better.files.{File => Path}

/**
  * @author uchida
  */
object IOUtil {

  def createDirectory(path: Path): Unit = {
    if (!path.isDirectory) path.createDirectory()
  }

  def input(path: Path): Option[String] = {
    if (!path.isDirectory) Some(path.lines().mkString)
    else None
  }

  def output(path: Path, content: String) = {
    if (path.isDirectory) path.createFile()
    path.write(content)
  }

  def directoryPaths(path: Path): Seq[Path] = {
    if (path.isDirectory)
      path.listRecursively().filter(_.isDirectory).toSeq
    else Seq.empty[Path]
  }

  def isDirectory(path: String): Boolean = {
    val filePath: Path = Path(path).path.toAbsolutePath
    filePath.isDirectory
  }

  def rename(from: Path, to: Path) =
    from.renameTo(to.name)

}
