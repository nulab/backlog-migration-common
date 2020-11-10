package com.nulabinc.backlog.migration.importer.service

import javax.inject.Inject

import better.files.{File => Path}
import com.nulabinc.backlog.migration.common.conf.{BacklogConstantValue, BacklogPaths}
import com.nulabinc.backlog.migration.common.convert.BacklogUnmarshaller
import com.nulabinc.backlog.migration.common.domain.{BacklogAttachment, BacklogWiki}
import com.nulabinc.backlog.migration.common.service.{
  AttachmentService,
  PropertyResolver,
  SharedFileService,
  WikiService
}
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, IOUtil, Logging, ProgressBar}
import com.osinka.i18n.Messages

/**
 * @author uchida
 */
private[importer] class WikisImporter @Inject() (
    backlogPaths: BacklogPaths,
    wikiService: WikiService,
    sharedFileService: SharedFileService,
    attachmentService: AttachmentService
) extends Logging {

  def execute(projectId: Long, propertyResolver: PropertyResolver) = {
    val paths    = IOUtil.directoryPaths(backlogPaths.wikiDirectoryPath)
    val allWikis = wikiService.allWikis()

    def exists(wikiName: String): Boolean = {
      allWikis.exists(wiki => wiki.name == wikiName)
    }

    def condition(path: Path): Boolean = {
      unmarshal(path) match {
        case Some(wiki) =>
          if (wiki.name == BacklogConstantValue.WIKI_HOME_NAME) false
          else exists(wiki.name)
        case _ => false
      }
    }

    val console = (ProgressBar.progress _)(
      Messages("common.wikis"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    val wikiDirs = paths.filterNot(condition)
    wikiDirs.zipWithIndex.foreach {
      case (wikiDir, index) =>
        for {
          wiki    <- unmarshal(wikiDir)
          created <- create(projectId, propertyResolver, wiki)
        } yield postCreate(created.id, wikiDir, wiki)
        console(index + 1, wikiDirs.size)
    }
  }

  private[this] def create(
      projectId: Long,
      propertyResolver: PropertyResolver,
      wiki: BacklogWiki
  ): Option[BacklogWiki] = {
    if (wiki.name == BacklogConstantValue.WIKI_HOME_NAME)
      wikiService.update(wiki)
    else
      Some(wikiService.create(projectId, wiki, propertyResolver))
  }

  private[this] def postCreate(
      createdId: Long,
      wikiDir: Path,
      wiki: BacklogWiki
  ) = {
    wikiService.addAttachment(createdId, postAttachments(wikiDir, wiki)) match {
      case Right(_) =>
      case Left(e) =>
        ConsoleOut.error(
          Messages("import.error.wiki.attachment", wiki.name, e.getMessage)
        )
    }
    sharedFileService.linkWikiSharedFile(createdId, wiki)
  }

  private[this] def postAttachments(
      wikiDir: Path,
      wiki: BacklogWiki
  ): Seq[BacklogAttachment] = {
    val paths =
      wiki.attachments.flatMap(attachment => toPath(attachment, wikiDir))
    paths.flatMap { path =>
      attachmentService.postAttachment(path.pathAsString) match {
        case Right(attachment) => Some(attachment)
        case Left(e) =>
          if (e.getMessage.contains("The size of attached file is too large."))
            ConsoleOut.error(
              Messages("import.error.attachment.too_large", path.name)
            )
          else
            ConsoleOut.error(
              Messages("import.error.issue.attachment", path.name, e.getMessage)
            )
          None
      }
    }
  }

  private[this] def toPath(
      attachment: BacklogAttachment,
      wikiDir: Path
  ): Option[Path] = {
    val files = backlogPaths.wikiAttachmentPath(wikiDir).list
    files.find(file => file.name == attachment.name)
  }

  private[this] def unmarshal(path: Path): Option[BacklogWiki] =
    BacklogUnmarshaller.wiki(backlogPaths.wikiJson(path))

}
