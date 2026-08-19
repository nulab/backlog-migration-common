package com.nulabinc.backlog.migration.common.service

import java.io.{File, FileInputStream, InputStream}
import java.lang.Thread.sleep
import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.{
  DocumentCommentWrites,
  DocumentTreeWrites,
  DocumentWrites
}
import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentCommentReply,
  BacklogDocumentTag,
  BacklogDocumentTree,
  BacklogUser
}
import com.nulabinc.backlog.migration.common.utils.{FileUtil, Logging}
import com.nulabinc.backlog4j.api.option.{
  AddDocumentTagsParams,
  GetDocumentTreeParams,
  GetDocumentsCountParams,
  GetDocumentsParams
}
import com.nulabinc.backlog4j.internal.file.AttachmentDataImpl
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.jdk.CollectionConverters._
import scala.util.Using

/**
 * @author
 *   nulab
 */
class DocumentServiceImpl @Inject() (implicit
    val documentWrites: DocumentWrites,
    val documentCommentWrites: DocumentCommentWrites,
    val documentTreeWrites: DocumentTreeWrites,
    backlog: BacklogAPIClient
) extends DocumentService
    with Logging {

  override def allDocuments(
      projectId: Long,
      offset: Int,
      count: Int
  ): Seq[BacklogDocument] = {
    val params = new GetDocumentsParams(List[java.lang.Long](projectId).asJava, offset.toLong)
    params.count(count)
    params.sort(GetDocumentsParams.SortKey.Created)
    params.order(GetDocumentsParams.Order.Asc)
    try {
      backlog
        .getDocuments(params)
        .asScala
        .toSeq
        .map(document => withComments(Convert.toBacklog(document)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[BacklogDocument]
    }
  }

  override def countDocuments(projectId: Long): Int = {
    val params = new GetDocumentsCountParams(projectId)
    try {
      backlog.getDocumentsCount(params)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        0
    }
  }

  override def documentOfId(documentId: String): BacklogDocument = {
    sleep(500)
    withComments(Convert.toBacklog(backlog.getDocument(documentId)))
  }

  override def documentTree(projectId: Long): BacklogDocumentTree = {
    val params = new GetDocumentTreeParams(java.lang.Long.valueOf(projectId))
    Convert.toBacklog(backlog.getDocumentTree(params))
  }

  override def downloadDocumentAttachment(
      documentId: String,
      attachmentId: Long
  ): Option[(String, InputStream)] =
    try {
      val attachmentData = backlog.downloadDocumentAttachment(documentId, attachmentId)
      if (attachmentData.getFilename.isEmpty) {
        None
      } else {
        Some((attachmentData.getFilename, attachmentData.getContent))
      }
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        None
    }

  override def create(
      projectId: Long,
      document: BacklogDocument,
      optParentId: Option[String],
      addLast: Boolean,
      propertyResolver: PropertyResolver
  ): String = {
    val jsonBody =
      createDocumentJson(projectId, document, optParentId, addLast, propertyResolver).compactPrint
    val response = backlog.importDocument(jsonBody)
    JsonParser(response).asJsObject.fields("id").convertTo[String]
  }

  override def updateContent(
      documentId: String,
      document: BacklogDocument,
      propertyResolver: PropertyResolver
  ): Unit = {
    val jsonBody = updateContentJson(document, propertyResolver).compactPrint
    backlog.importUpdateDocumentContent(documentId, jsonBody)
  }

  override def addComment(
      documentId: String,
      comment: BacklogDocumentComment,
      propertyResolver: PropertyResolver
  ): Either[Throwable, String] =
    try {
      val jsonBody = commentJson(comment, propertyResolver).compactPrint
      val response = backlog.importDocumentComment(documentId, jsonBody)
      Right(JsonParser(response).asJsObject.fields("id").convertTo[String])
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }

  def createDocumentJson(
      projectId: Long,
      document: BacklogDocument,
      optParentId: Option[String],
      addLast: Boolean,
      propertyResolver: PropertyResolver
  ): JsObject = {
    val fields = scala.collection.mutable.Map[String, JsValue](
      "projectId" -> JsNumber(projectId),
      "title"     -> JsString(document.title),
      "addLast"   -> JsBoolean(addLast)
    )
    document.optEmoji.foreach(emoji => fields += "emoji" -> JsString(emoji))
    optParentId.foreach(parentId => fields += "parentId" -> JsString(parentId))
    document.optCreated.foreach(created => fields += "created" -> JsString(created))
    resolvedUserId(document.optCreatedUser, propertyResolver)
      .foreach(id => fields += "createdUserId" -> JsNumber(id))
    document.optUpdated.foreach(updated => fields += "updated" -> JsString(updated))
    resolvedUserId(document.optUpdatedUser, propertyResolver)
      .foreach(id => fields += "updatedUserId" -> JsNumber(id))
    JsObject(fields.toMap)
  }

  def updateContentJson(
      document: BacklogDocument,
      propertyResolver: PropertyResolver
  ): JsObject = {
    val fields = scala.collection.mutable.Map[String, JsValue](
      "json"  -> document.optJson.map(_.parseJson).getOrElse(JsObject.empty),
      "plain" -> JsString(document.optPlain.getOrElse(""))
    )
    document.optUpdated.foreach(updated => fields += "updated" -> JsString(updated))
    resolvedUserId(document.optUpdatedUser, propertyResolver)
      .foreach(id => fields += "updatedUserId" -> JsNumber(id))
    JsObject(fields.toMap)
  }

  def commentJson(
      comment: BacklogDocumentComment,
      propertyResolver: PropertyResolver
  ): JsObject = {
    val fields = scala.collection.mutable.Map[String, JsValue](
      "content"     -> JsString(comment.content),
      "plain"       -> JsString(comment.plain),
      "statusId"    -> JsNumber(comment.statusId),
      "commentType" -> JsString(comment.commentType)
    )
    comment.optCreated.foreach(created => fields += "created" -> JsString(created))
    resolvedUserId(comment.optCreatedUser, propertyResolver)
      .foreach(id => fields += "createdUserId" -> JsNumber(id))
    comment.optUpdated.foreach(updated => fields += "updated" -> JsString(updated))
    if (comment.replies.nonEmpty) {
      fields += "replies" -> JsArray(
        comment.replies.map(replyFields(_, propertyResolver)).toVector
      )
    }
    JsObject(fields.toMap)
  }

  override def addAttachment(
      documentId: String,
      path: String
  ): Either[Throwable, BacklogAttachment] = {
    sleep(500)
    val file = new File(path)
    try {
      val attachment = Using.resource(new FileInputStream(file)) { inputStream =>
        val attachmentData = new AttachmentDataImpl(file.getName, inputStream)
        backlog.addDocumentAttachment(documentId, attachmentData)
      }
      Right(
        BacklogAttachment(
          optId = Some(attachment.getId),
          name = FileUtil.clean(attachment.getName)
        )
      )
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

  override def addTags(
      documentId: String,
      tagNames: Seq[String]
  ): Either[Throwable, Seq[BacklogDocumentTag]] =
    try {
      if (tagNames.isEmpty) {
        Right(Seq.empty)
      } else {
        val params = new AddDocumentTagsParams(tagNames.asJava)
        val tags   = backlog.addDocumentTags(documentId, params).asScala.toSeq
        Right(tags.map(tag => BacklogDocumentTag(id = tag.getId, name = tag.getName)))
      }
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }

  private[this] def resolvedUserId(
      optUser: Option[BacklogUser],
      propertyResolver: PropertyResolver
  ): Option[Long] =
    for {
      user   <- optUser
      userId <- user.optUserId
      id     <- propertyResolver.optResolvedUserId(userId)
    } yield id

  private[this] def replyFields(
      reply: BacklogDocumentCommentReply,
      propertyResolver: PropertyResolver
  ): JsObject = {
    val fields = scala.collection.mutable.Map[String, JsValue](
      "content" -> JsString(reply.content),
      "plain"   -> JsString(reply.plain)
    )
    reply.optCreated.foreach(created => fields += "created" -> JsString(created))
    resolvedUserId(reply.optCreatedUser, propertyResolver)
      .foreach(id => fields += "createdUserId" -> JsNumber(id))
    reply.optUpdated.foreach(updated => fields += "updated" -> JsString(updated))
    JsObject(fields.toMap)
  }

  override def rewriteInlineCommentIds(
      document: BacklogDocument,
      commentIdMap: Map[String, String]
  ): BacklogDocument =
    if (commentIdMap.isEmpty) document
    else
      document.optJson match {
        case Some(json) =>
          document.copy(optJson = Some(rewriteJsValue(json.parseJson, commentIdMap).compactPrint))
        case None => document
      }

  private[this] def rewriteJsValue(value: JsValue, commentIdMap: Map[String, String]): JsValue =
    value match {
      case JsObject(fields) =>
        val rewritten = fields.map { case (key, v) => key -> rewriteJsValue(v, commentIdMap) }
        rewritten.get("type") match {
          case Some(JsString("inlineComment")) =>
            rewritten.get("attrs") match {
              case Some(attrs: JsObject) =>
                JsObject(
                  rewritten.updated("attrs", rewriteInlineCommentAttrs(attrs, commentIdMap))
                )
              case _ => JsObject(rewritten)
            }
          case _ => JsObject(rewritten)
        }
      case JsArray(elements) => JsArray(elements.map(rewriteJsValue(_, commentIdMap)))
      case other             => other
    }

  private[this] def rewriteInlineCommentAttrs(
      attrs: JsObject,
      commentIdMap: Map[String, String]
  ): JsObject =
    attrs.fields.get("comment") match {
      case Some(comment: JsObject) =>
        comment.fields.get("id") match {
          case Some(JsString(oldCommentId)) =>
            commentIdMap.get(oldCommentId) match {
              case Some(newCommentId) =>
                JsObject(
                  attrs.fields.updated(
                    "comment",
                    JsObject(comment.fields.updated("id", JsString(newCommentId)))
                  )
                )
              case None =>
                logger.warn(
                  s"No migrated comment id found for inline comment mark (id=$oldCommentId)"
                )
                attrs
            }
          case _ => attrs
        }
      case _ => attrs
    }

  private[this] def withComments(document: BacklogDocument): BacklogDocument = {
    val comments =
      try {
        backlog.getDocumentComments(document.id).asScala.toSeq.map(Convert.toBacklog(_))
      } catch {
        case e: Throwable =>
          logger.error(
            s"Failed to fetch comments for document ${document.id}: ${e.getMessage}",
            e
          )
          Seq.empty
      }
    document.copy(comments = comments)
  }

}
