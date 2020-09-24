package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.{BacklogWiki, BacklogWikiTag}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging, StringUtil}
import com.nulabinc.backlog4j.{Attachment, SharedFile, Wiki, WikiTag}

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
private[common] class WikiWrites @Inject() (
    implicit val userWrites: UserWrites,
    implicit val sharedFileWrites: SharedFileWrites,
    implicit val attachmentWrites: AttachmentWrites
) extends Writes[Wiki, BacklogWiki]
    with Logging {

  override def writes(wiki: Wiki): BacklogWiki = {
    if (getSharedFiles(wiki).nonEmpty) {
      logger.debug("[SharedFiles]issue shared files not empty.")
    }

    BacklogWiki(
      optId = Some(wiki.getId),
      name = StringUtil.toSafeString(wiki.getName),
      optContent = Option(wiki.getContent).map(StringUtil.toSafeString),
      attachments = getAttachments(wiki).map(Convert.toBacklog(_)),
      sharedFiles = getSharedFiles(wiki).map(Convert.toBacklog(_)),
      tags = wiki.getTags.asScala.toSeq.map(convertTag),
      optCreatedUser = Option(wiki.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(wiki.getCreated).map(DateUtil.isoFormat),
      optUpdatedUser = Option(wiki.getUpdatedUser).map(Convert.toBacklog(_)),
      optUpdated = Option(wiki.getUpdated).map(DateUtil.isoFormat)
    )
  }

  private[this] def getSharedFiles(wiki: Wiki): Seq[SharedFile] = {
    try {
      wiki.getSharedFiles.asScala.toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[SharedFile]
    }
  }

  private[this] def getAttachments(wiki: Wiki): Seq[Attachment] = {
    try {
      wiki.getAttachments.asScala.toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[Attachment]
    }
  }

  private[this] def convertTag(tag: WikiTag): BacklogWikiTag =
    BacklogWikiTag(id = tag.getId, name = tag.getName)

}
