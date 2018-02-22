package com.nulabinc.backlog.migration.common.utils

import java.nio.charset.Charset

import better.files.{File => Path}

/**
  * @author uchida
  */
object IOUtil {

  def createDirectory(path: Path): Unit =
    path.createDirectories()

  def input(path: Path): Option[String] = {
    if (!path.isDirectory && path.exists) Some(path.lines(charset = Charset.defaultCharset()).mkString)
    else None
  }

  def output(path: Path, content: String) = {
    if (!path.exists) {
      path.parent.toJava.mkdirs()
    }
    path.write(content)
  }

  def directoryPaths(path: Path): Seq[Path] = {
    if (path.isDirectory)
      path.list.filter(_.isDirectory).toSeq
    else Seq.empty[Path]
  }

  def isDirectory(path: String): Boolean = {
    val filePath: Path = Path(path).path.toAbsolutePath
    filePath.isDirectory
  }

  def rename(from: Path, to: Path) =
    from.renameTo(to.name)

}
