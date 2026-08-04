package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.{
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentCommentReply,
  BacklogDocumentTag,
  BacklogDocumentTree,
  BacklogDocumentTreeNode
}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging, StringUtil}
import com.nulabinc.backlog4j.{
  Document,
  DocumentComment,
  DocumentCommentReply,
  DocumentTag,
  DocumentTree,
  DocumentTreeNode
}

import scala.jdk.CollectionConverters._

/**
 * @author
 *   nulab
 */
private[common] class DocumentWrites @Inject() (
    implicit val userWrites: UserWrites,
    implicit val attachmentWrites: AttachmentWrites
) extends Writes[Document, BacklogDocument]
    with Logging {

  override def writes(document: Document): BacklogDocument =
    BacklogDocument(
      optId = Some(document.getId),
      projectId = document.getProjectId,
      title = StringUtil.toSafeString(document.getTitle),
      optJson = Option(document.getJson),
      optPlain = Option(document.getPlain).map(StringUtil.toSafeString),
      optEmoji = Option(document.getEmoji),
      tags = document.getTags.asScala.toSeq.map(convertTag),
      attachments = document.getAttachments.asScala.toSeq.map(Convert.toBacklog(_)),
      comments = Seq.empty,
      optCreatedUser = Option(document.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(document.getCreated).map(DateUtil.isoFormat),
      optUpdatedUser = Option(document.getUpdatedUser).map(Convert.toBacklog(_)),
      optUpdated = Option(document.getUpdated).map(DateUtil.isoFormat)
    )

  private[this] def convertTag(tag: DocumentTag): BacklogDocumentTag =
    BacklogDocumentTag(id = tag.getId, name = tag.getName)

}

private[common] class DocumentCommentWrites @Inject() (implicit
    val userWrites: UserWrites
) extends Writes[DocumentComment, BacklogDocumentComment]
    with Logging {

  override def writes(comment: DocumentComment): BacklogDocumentComment =
    BacklogDocumentComment(
      optId = Some(comment.getId),
      statusId = comment.getStatusId,
      content = StringUtil.toSafeString(comment.getContent),
      plain = StringUtil.toSafeString(comment.getPlain),
      commentType = comment.getCommentType,
      optCreatedUser = Option(comment.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(comment.getCreated).map(DateUtil.isoFormat),
      optUpdated = Option(comment.getUpdated).map(DateUtil.isoFormat),
      replies = Option(comment.getReplies)
        .map(_.asScala.toSeq.map(writesReply))
        .getOrElse(Seq.empty)
    )

  private[this] def writesReply(reply: DocumentCommentReply): BacklogDocumentCommentReply =
    BacklogDocumentCommentReply(
      optId = Some(reply.getId),
      content = StringUtil.toSafeString(reply.getContent),
      plain = StringUtil.toSafeString(reply.getPlain),
      optCreatedUser = Option(reply.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(reply.getCreated).map(DateUtil.isoFormat),
      optUpdated = Option(reply.getUpdated).map(DateUtil.isoFormat)
    )

}

private[common] class DocumentTreeWrites @Inject() ()
    extends Writes[DocumentTree, BacklogDocumentTree]
    with Logging {

  override def writes(tree: DocumentTree): BacklogDocumentTree =
    BacklogDocumentTree(
      projectId = tree.getProjectId,
      activeTree = convertNode(tree.getActiveTree),
      trashTree = convertNode(tree.getTrashTree)
    )

  private[this] def convertNode(node: DocumentTreeNode): BacklogDocumentTreeNode =
    BacklogDocumentTreeNode(
      id = node.getId,
      name = StringUtil.toSafeString(node.getName),
      optEmoji = Option(node.getEmoji),
      children = node.getChildren.asScala.toSeq.map(convertNode)
    )

}
