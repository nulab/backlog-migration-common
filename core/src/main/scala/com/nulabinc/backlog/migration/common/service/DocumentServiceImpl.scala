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
      isTrash: Boolean,
      propertyResolver: PropertyResolver
  ): String = {
    val jsonBody =
      createDocumentJson(
        projectId,
        document,
        optParentId,
        addLast,
        isTrash,
        propertyResolver
      ).compactPrint
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
      isTrash: Boolean,
      propertyResolver: PropertyResolver
  ): JsObject = {
    val fields = scala.collection.mutable.Map[String, JsValue](
      "projectId" -> JsNumber(projectId),
      "title"     -> JsString(document.title),
      "addLast"   -> JsBoolean(addLast),
      "isTrash"   -> JsBoolean(isTrash)
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

  override def rewriteIssueMentions(
      document: BacklogDocument,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, IssueMentionRewriteStats) =
    if (issueIdMap.isEmpty && issueKeyMap.isEmpty) (document, IssueMentionRewriteStats(0, 0, 0, 0))
    else
      document.optJson match {
        case Some(json) =>
          val plainTextReplacements =
            scala.collection.mutable.ArrayBuffer.empty[(String, String, String)]
          val ctx = IssueMentionContext(
            issueIdMap,
            issueKeyMap,
            srcProjectKey,
            dstProjectId,
            dstProjectKey,
            plainTextReplacements
          )
          val newJson = rewriteIssueMentionJsValue(json.parseJson, ctx).compactPrint
          val newPlain =
            document.optPlain.map(rewritePlainTextIssueMentions(_, plainTextReplacements.toSeq))
          val stats = IssueMentionRewriteStats(
            total = ctx.rewrittenCount + ctx.skippedExternalProjectCount + ctx.unresolvedCount,
            rewritten = ctx.rewrittenCount,
            skippedExternalProject = ctx.skippedExternalProjectCount,
            unresolved = ctx.unresolvedCount
          )
          (document.copy(optJson = Some(newJson), optPlain = newPlain), stats)
        case None => (document, IssueMentionRewriteStats(0, 0, 0, 0))
      }

  private[this] case class IssueMentionContext(
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String,
      // (oldId, old tag text, new tag text) captured for each resolved mention, applied to optPlain afterwards
      plainTextReplacements: scala.collection.mutable.ArrayBuffer[(String, String, String)]
  ) {
    var rewrittenCount: Int              = 0
    var skippedExternalProjectCount: Int = 0
    var unresolvedCount: Int             = 0
  }

  private[this] def rewritePlainTextIssueMentions(
      plain: String,
      replacements: Seq[(String, String, String)]
  ): String =
    replacements.foldLeft(plain) {
      case (text, (oldId, oldTag, newTag)) =>
        replaceFirstOccurrence(text, oldTag, newTag) match {
          case Some(newText) => newText
          case None =>
            logger.warn(
              s"Could not find expected issue mention text in document plain text (id=$oldId) — plain text left unchanged for this mention"
            )
            text
        }
    }

  // Replaces only the first remaining occurrence of oldTag, so that N
  // identical mentions consume exactly N occurrences in order rather than
  // String.replace's all-at-once semantics collapsing them into one.
  private[this] def replaceFirstOccurrence(
      text: String,
      oldTag: String,
      newTag: String
  ): Option[String] = {
    val idx = text.indexOf(oldTag)
    if (idx < 0) None
    else Some(text.substring(0, idx) + newTag + text.substring(idx + oldTag.length))
  }

  private[this] def issueMentionTagText(fields: Map[String, JsValue]): Option[String] =
    for {
      id          <- fields.get("id").collect { case JsString(s) => s }
      label       <- fields.get("label").collect { case JsString(s) => s }
      mentionType <- fields.get("mentionType").collect { case JsString(s) => s }
      projectKey  <- fields.get("projectKey").collect { case JsString(s) => s }
    } yield {
      val sb = new StringBuilder("[issueMention id=\"")
        .append(id)
        .append("\" label=\"")
        .append(label)
        .append("\" mentionType=\"")
        .append(mentionType)
        .append("\" projectKey=\"")
        .append(projectKey)
        .append("\"")
      fields.get("projectId").collect { case JsNumber(n) => n }.foreach { n =>
        sb.append(" projectId=\"").append(n.toString).append("\"")
      }
      fields.get("issueId").collect { case JsNumber(n) => n }.foreach { n =>
        sb.append(" issueId=\"").append(n.toString).append("\"")
      }
      sb.append("]")
      sb.toString
    }

  private[this] def rewriteIssueMentionJsValue(value: JsValue, ctx: IssueMentionContext): JsValue =
    value match {
      case JsObject(fields) =>
        val rewritten = fields.map { case (key, v) => key -> rewriteIssueMentionJsValue(v, ctx) }
        rewritten.get("type") match {
          case Some(JsString("issueMention")) =>
            rewritten.get("attrs") match {
              case Some(attrs: JsObject) =>
                JsObject(rewritten.updated("attrs", rewriteIssueMentionAttrs(attrs, ctx)))
              case _ => JsObject(rewritten)
            }
          case _ => JsObject(rewritten)
        }
      case JsArray(elements) => JsArray(elements.map(rewriteIssueMentionJsValue(_, ctx)))
      case other             => other
    }

  private[this] def rewriteIssueMentionAttrs(
      attrs: JsObject,
      ctx: IssueMentionContext
  ): JsObject = {
    val optOldId      = attrs.fields.get("id").collect { case JsString(id) => id }
    val optProjectKey = attrs.fields.get("projectKey").collect { case JsString(pk) => pk }

    (optOldId, optProjectKey) match {
      case (Some(oldId), Some(projectKey)) if projectKey == ctx.srcProjectKey =>
        resolveAndRewriteIssueMentionAttrs(attrs, oldId, ctx)
      case (Some(oldId), Some(projectKey)) =>
        ctx.skippedExternalProjectCount += 1
        logger.warn(
          s"Skipping issue mention for external project (projectKey=$projectKey, id=$oldId) — not part of this migration"
        )
        attrs
      case _ => attrs
    }
  }

  private[this] def resolveAndRewriteIssueMentionAttrs(
      attrs: JsObject,
      oldId: String,
      ctx: IssueMentionContext
  ): JsObject = {
    val optOldIssueId = attrs.fields.get("issueId").collect { case JsNumber(n) => n.toLong }
    val optNewIssueId = optOldIssueId.flatMap(ctx.issueIdMap.get)
    val optNewKey     = ctx.issueKeyMap.get(oldId)

    if (optNewIssueId.isEmpty && optNewKey.isEmpty) {
      ctx.unresolvedCount += 1
      logger.warn(
        s"No migrated issue found for issue mention (id=$oldId, issueId=$optOldIssueId) — leaving reference unresolved"
      )
      attrs
    } else {
      ctx.rewrittenCount += 1
      var fields = attrs.fields.updated("id", JsString(optNewKey.getOrElse(oldId)))
      if (attrs.fields.contains("issueId")) {
        fields = fields.updated(
          "issueId",
          optNewIssueId.map(JsNumber(_)).getOrElse(attrs.fields("issueId"))
        )
      }
      if (attrs.fields.contains("projectId")) {
        fields = fields.updated("projectId", JsNumber(ctx.dstProjectId))
      }
      fields = fields.updated("projectKey", JsString(ctx.dstProjectKey))

      (issueMentionTagText(attrs.fields), issueMentionTagText(fields)) match {
        case (Some(oldTag), Some(newTag)) => ctx.plainTextReplacements += ((oldId, oldTag, newTag))
        case _                            => ()
      }

      JsObject(fields)
    }
  }

  private[this] val documentMentionUrlPattern =
    """^(https?://[^/]+/document/)([^/]+)(/e)?/([A-Za-z0-9]+)$""".r

  override def rewriteDocumentMentions(
      document: BacklogDocument,
      documentIdMap: Map[String, String],
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, DocumentMentionRewriteStats) =
    if (documentIdMap.isEmpty) (document, DocumentMentionRewriteStats(0, 0, 0, 0))
    else
      document.optJson match {
        case Some(json) =>
          val plainTextReplacements =
            scala.collection.mutable.ArrayBuffer.empty[(String, String, String)]
          val ctx = DocumentMentionContext(
            documentIdMap,
            srcProjectKey,
            dstProjectId,
            dstProjectKey,
            plainTextReplacements
          )
          val newJson = rewriteDocumentMentionJsValue(json.parseJson, ctx).compactPrint
          val newPlain =
            document.optPlain.map(rewritePlainTextDocumentMentions(_, plainTextReplacements.toSeq))
          val stats = DocumentMentionRewriteStats(
            total = ctx.rewrittenCount + ctx.skippedExternalProjectCount + ctx.unresolvedCount,
            rewritten = ctx.rewrittenCount,
            skippedExternalProject = ctx.skippedExternalProjectCount,
            unresolved = ctx.unresolvedCount
          )
          (document.copy(optJson = Some(newJson), optPlain = newPlain), stats)
        case None => (document, DocumentMentionRewriteStats(0, 0, 0, 0))
      }

  private[this] case class DocumentMentionContext(
      documentIdMap: Map[String, String],
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String,
      // (oldId, old tag text, new tag text) captured for each resolved mention, applied to optPlain afterwards
      plainTextReplacements: scala.collection.mutable.ArrayBuffer[(String, String, String)]
  ) {
    var rewrittenCount: Int              = 0
    var skippedExternalProjectCount: Int = 0
    var unresolvedCount: Int             = 0
  }

  private[this] def rewritePlainTextDocumentMentions(
      plain: String,
      replacements: Seq[(String, String, String)]
  ): String =
    replacements.foldLeft(plain) {
      case (text, (oldId, oldTag, newTag)) =>
        replaceFirstOccurrence(text, oldTag, newTag) match {
          case Some(newText) => newText
          case None =>
            logger.warn(
              s"Could not find expected document mention text in document plain text (id=$oldId) — plain text left unchanged for this mention"
            )
            text
        }
    }

  private[this] def documentMentionTagText(fields: Map[String, JsValue]): Option[String] =
    for {
      id          <- fields.get("id").collect { case JsString(s) => s }
      label       <- fields.get("label").collect { case JsString(s) => s }
      mentionType <- fields.get("mentionType").collect { case JsString(s) => s }
      projectKey  <- fields.get("projectKey").collect { case JsString(s) => s }
    } yield {
      val sb = new StringBuilder("[documentMention id=\"")
        .append(id)
        .append("\" label=\"")
        .append(label)
        .append("\" mentionType=\"")
        .append(mentionType)
        .append("\" projectKey=\"")
        .append(projectKey)
        .append("\"")
      fields.get("projectId").collect { case JsNumber(n) => n }.foreach { n =>
        sb.append(" projectId=\"").append(n.toString).append("\"")
      }
      fields.get("url").collect { case JsString(s) => s }.foreach { u =>
        sb.append(" url=\"").append(u).append("\"")
      }
      sb.append("]")
      sb.toString
    }

  private[this] def rewriteDocumentMentionJsValue(
      value: JsValue,
      ctx: DocumentMentionContext
  ): JsValue =
    value match {
      case JsObject(fields) =>
        val rewritten = fields.map {
          case (key, v) => key -> rewriteDocumentMentionJsValue(v, ctx)
        }
        rewritten.get("type") match {
          case Some(JsString("documentMention")) =>
            rewritten.get("attrs") match {
              case Some(attrs: JsObject) =>
                JsObject(rewritten.updated("attrs", rewriteDocumentMentionAttrs(attrs, ctx)))
              case _ => JsObject(rewritten)
            }
          case _ => JsObject(rewritten)
        }
      case JsArray(elements) => JsArray(elements.map(rewriteDocumentMentionJsValue(_, ctx)))
      case other             => other
    }

  private[this] def rewriteDocumentMentionAttrs(
      attrs: JsObject,
      ctx: DocumentMentionContext
  ): JsObject = {
    val optOldId      = attrs.fields.get("id").collect { case JsString(id) => id }
    val optProjectKey = attrs.fields.get("projectKey").collect { case JsString(pk) => pk }

    (optOldId, optProjectKey) match {
      case (Some(oldId), Some(projectKey)) if projectKey == ctx.srcProjectKey =>
        resolveAndRewriteDocumentMentionAttrs(attrs, oldId, ctx)
      case (Some(oldId), Some(projectKey)) =>
        ctx.skippedExternalProjectCount += 1
        logger.warn(
          s"Skipping document mention for external project (projectKey=$projectKey, id=$oldId) — not part of this migration"
        )
        attrs
      case _ => attrs
    }
  }

  private[this] def resolveAndRewriteDocumentMentionAttrs(
      attrs: JsObject,
      oldId: String,
      ctx: DocumentMentionContext
  ): JsObject =
    ctx.documentIdMap.get(oldId) match {
      case None =>
        ctx.unresolvedCount += 1
        logger.warn(
          s"No migrated document found for document mention (id=$oldId) — leaving reference unresolved"
        )
        attrs
      case Some(newId) =>
        ctx.rewrittenCount += 1
        var fields = attrs.fields.updated("id", JsString(newId))
        if (fields.contains("projectId")) {
          fields = fields.updated("projectId", JsNumber(ctx.dstProjectId))
        }
        fields = fields.updated("projectKey", JsString(ctx.dstProjectKey))
        fields.get("url").collect { case JsString(url) => url }.filter(_.nonEmpty).foreach { url =>
          rewriteDocumentMentionUrl(url, ctx.dstProjectKey, newId) match {
            case Some(newUrl) => fields = fields.updated("url", JsString(newUrl))
            case None =>
              logger.warn(
                s"Document mention url did not match the expected format (id=$oldId, url=$url) — url left unchanged"
              )
          }
        }

        (documentMentionTagText(attrs.fields), documentMentionTagText(fields)) match {
          case (Some(oldTag), Some(newTag)) =>
            ctx.plainTextReplacements += ((oldId, oldTag, newTag))
          case _ => ()
        }

        JsObject(fields)
    }

  private[this] def rewriteDocumentMentionUrl(
      url: String,
      dstProjectKey: String,
      newDocumentId: String
  ): Option[String] =
    url match {
      case documentMentionUrlPattern(prefix, _, eSegment, _) =>
        Some(s"$prefix$dstProjectKey${Option(eSegment).getOrElse("")}/$newDocumentId")
      case _ => None
    }

  override def rewriteMentions(
      document: BacklogDocument,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      documentIdMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, IssueMentionRewriteStats, DocumentMentionRewriteStats) = {
    val doIssueRewrite    = issueIdMap.nonEmpty || issueKeyMap.nonEmpty
    val doDocumentRewrite = documentIdMap.nonEmpty

    if (!doIssueRewrite && !doDocumentRewrite) {
      (document, IssueMentionRewriteStats(0, 0, 0, 0), DocumentMentionRewriteStats(0, 0, 0, 0))
    } else
      document.optJson match {
        case Some(json) =>
          val issuePlainTextReplacements =
            scala.collection.mutable.ArrayBuffer.empty[(String, String, String)]
          val documentPlainTextReplacements =
            scala.collection.mutable.ArrayBuffer.empty[(String, String, String)]

          val optIssueCtx =
            if (doIssueRewrite)
              Some(
                IssueMentionContext(
                  issueIdMap,
                  issueKeyMap,
                  srcProjectKey,
                  dstProjectId,
                  dstProjectKey,
                  issuePlainTextReplacements
                )
              )
            else None
          val optDocumentCtx =
            if (doDocumentRewrite)
              Some(
                DocumentMentionContext(
                  documentIdMap,
                  srcProjectKey,
                  dstProjectId,
                  dstProjectKey,
                  documentPlainTextReplacements
                )
              )
            else None

          val newJson =
            rewriteMentionsJsValue(json.parseJson, optIssueCtx, optDocumentCtx).compactPrint
          val newPlain = document.optPlain
            .map(rewritePlainTextIssueMentions(_, issuePlainTextReplacements.toSeq))
            .map(rewritePlainTextDocumentMentions(_, documentPlainTextReplacements.toSeq))

          val issueStats = optIssueCtx match {
            case Some(ctx) =>
              IssueMentionRewriteStats(
                total = ctx.rewrittenCount + ctx.skippedExternalProjectCount + ctx.unresolvedCount,
                rewritten = ctx.rewrittenCount,
                skippedExternalProject = ctx.skippedExternalProjectCount,
                unresolved = ctx.unresolvedCount
              )
            case None => IssueMentionRewriteStats(0, 0, 0, 0)
          }
          val documentStats = optDocumentCtx match {
            case Some(ctx) =>
              DocumentMentionRewriteStats(
                total = ctx.rewrittenCount + ctx.skippedExternalProjectCount + ctx.unresolvedCount,
                rewritten = ctx.rewrittenCount,
                skippedExternalProject = ctx.skippedExternalProjectCount,
                unresolved = ctx.unresolvedCount
              )
            case None => DocumentMentionRewriteStats(0, 0, 0, 0)
          }

          (document.copy(optJson = Some(newJson), optPlain = newPlain), issueStats, documentStats)
        case None =>
          (document, IssueMentionRewriteStats(0, 0, 0, 0), DocumentMentionRewriteStats(0, 0, 0, 0))
      }
  }

  private[this] def rewriteMentionsJsValue(
      value: JsValue,
      optIssueCtx: Option[IssueMentionContext],
      optDocumentCtx: Option[DocumentMentionContext]
  ): JsValue =
    value match {
      case JsObject(fields) =>
        val rewritten = fields.map {
          case (key, v) => key -> rewriteMentionsJsValue(v, optIssueCtx, optDocumentCtx)
        }
        rewritten.get("type") match {
          case Some(JsString("issueMention")) if optIssueCtx.isDefined =>
            rewritten.get("attrs") match {
              case Some(attrs: JsObject) =>
                JsObject(
                  rewritten.updated("attrs", rewriteIssueMentionAttrs(attrs, optIssueCtx.get))
                )
              case _ => JsObject(rewritten)
            }
          case Some(JsString("documentMention")) if optDocumentCtx.isDefined =>
            rewritten.get("attrs") match {
              case Some(attrs: JsObject) =>
                JsObject(
                  rewritten
                    .updated("attrs", rewriteDocumentMentionAttrs(attrs, optDocumentCtx.get))
                )
              case _ => JsObject(rewritten)
            }
          case _ => JsObject(rewritten)
        }
      case JsArray(elements) =>
        JsArray(elements.map(rewriteMentionsJsValue(_, optIssueCtx, optDocumentCtx)))
      case other => other
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
