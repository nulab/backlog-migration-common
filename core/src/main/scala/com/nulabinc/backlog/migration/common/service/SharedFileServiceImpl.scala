package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import javax.inject.Inject
import com.nulabinc.backlog.migration.common.domain.{
  BacklogIssue,
  BacklogProjectKey,
  BacklogSharedFile,
  BacklogWiki
}
import com.nulabinc.backlog.migration.common.utils.Logging

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class SharedFileServiceImpl @Inject() (
    projectKey: BacklogProjectKey,
    backlog: BacklogAPIClient
) extends SharedFileService
    with Logging {

  override def linkIssueSharedFile(
      issueId: Long,
      backlogIssue: BacklogIssue
  ) = {
    val fileIds: Seq[Long] = backlogIssue.sharedFiles.flatMap(getFileId)
    if (fileIds.nonEmpty) {
      val links = backlog.getIssueSharedFiles(issueId).asScala
      val selectedLinks = fileIds.filter(fileId => {
        !links.exists(sharedFile => fileId == sharedFile.getId)
      })
      if (selectedLinks.nonEmpty) {
        backlog.linkIssueSharedFile(issueId, selectedLinks.asJava)
      }
    }
  }

  override def linkWikiSharedFile(wikiId: Long, backlogWiki: BacklogWiki) = {
    val fileIds: Seq[Long] = backlogWiki.sharedFiles.flatMap(getFileId)
    if (fileIds.nonEmpty) {
      val links = backlog.getWikiSharedFiles(wikiId).asScala
      val selectedLinks = fileIds.filter(fileId => {
        !links.exists(sharedFile => fileId == sharedFile.getId)
      })
      if (selectedLinks.nonEmpty) {
        backlog.linkWikiSharedFile(wikiId, fileIds.asJava)
      }
    }
  }

  private[this] def getFileId(
      backlogSharedFile: BacklogSharedFile
  ): Option[Long] = {
    val path =
      if (backlogSharedFile.dir.length > 0) backlogSharedFile.dir.substring(1)
      else backlogSharedFile.dir
    try {
      backlog
        .getSharedFiles(projectKey.value, path)
        .asScala
        .find(sharedFile => {
          backlogSharedFile.dir == sharedFile.getDir && backlogSharedFile.name == sharedFile.getName
        })
        .map(_.getId)
    } catch {
      case _: Throwable =>
        //TODO
        //logger.error(e.getMessage, e)
        None
    }
  }

}
