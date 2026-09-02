package com.nulabinc.backlog.migration.importer.service

import javax.inject.Inject

import better.files.{File => Path}
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.convert.BacklogUnmarshaller
import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogDocument,
  BacklogDocumentTreeNode,
  BacklogProject
}
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.service.{
  DocumentMentionRewriteStats,
  DocumentService,
  IssueMentionRewriteStats,
  PropertyResolver
}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.osinka.i18n.Messages
import monix.eval.Task
import monix.execution.Scheduler
import org.fusesource.jansi.Ansi.Color.GREEN

import scala.collection.mutable

/**
 * @author
 *   nulab
 */
private[importer] class DocumentsImporter @Inject() (
    backlogPaths: BacklogPaths,
    documentService: DocumentService
) extends Logging {

  def execute(
      project: BacklogProject,
      propertyResolver: PropertyResolver,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String
  )(implicit
      s: Scheduler,
      consoleDSL: ConsoleDSL[Task]
  ): Unit =
    BacklogUnmarshaller.documentTree(backlogPaths).foreach { tree =>
      val documentIdMap = mutable.Map.empty[String, String]
      val pending       = mutable.ArrayBuffer.empty[PendingDocument]

      // Phase 1: create every document across both trees first, since a
      // document's body may mention a sibling created later in tree order.
      createAll(
        tree.activeTree.children,
        None,
        isTrash = false,
        project,
        propertyResolver,
        documentIdMap,
        pending
      )
      createAll(
        tree.trashTree.children,
        None,
        isTrash = true,
        project,
        propertyResolver,
        documentIdMap,
        pending
      )

      val mentionRewriteContext = MentionRewriteContext(
        issueIdMap,
        issueKeyMap,
        srcProjectId,
        srcProjectKey,
        dstProjectId = project.id,
        dstProjectKey = project.key
      )
      val resolvedDocumentIdMap = documentIdMap.toMap

      // Phase 2: documentIdMap is now complete, so mentions can be resolved.
      pending.foreach { pendingDocument =>
        finalizeContent(
          pendingDocument,
          mentionRewriteContext,
          resolvedDocumentIdMap,
          propertyResolver
        ).runSyncUnsafe()
      }
    }

  private[this] case class MentionRewriteContext(
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  )

  // A created document awaiting phase 2's mention rewrite + content update.
  // `document` already has inlineComment marks rewritten (phase-1-local).
  private[this] case class PendingDocument(
      oldDocumentId: String,
      newDocumentId: String,
      document: BacklogDocument
  )

  private[this] def createAll(
      nodes: Seq[BacklogDocumentTreeNode],
      optNewParentId: Option[String],
      isTrash: Boolean,
      project: BacklogProject,
      propertyResolver: PropertyResolver,
      documentIdMap: mutable.Map[String, String],
      pending: mutable.ArrayBuffer[PendingDocument]
  )(implicit s: Scheduler, consoleDSL: ConsoleDSL[Task]): Unit =
    nodes.foreach { node =>
      val optNewId = unmarshal(node.id).map { document =>
        // isTrash is only consulted by the destination when optNewParentId is
        // empty (root of the subtree); it's harmless to pass through unconditionally.
        val newId = documentService.create(
          project.id,
          document,
          optNewParentId,
          addLast = true,
          isTrash = isTrash,
          propertyResolver
        )
        documentIdMap += node.id -> newId
        postCreate(node.id, newId, document, propertyResolver, pending).runSyncUnsafe()
        newId
      }
      // A failed/missing parent breaks the id mapping, so its children are skipped too.
      optNewId.foreach { newId =>
        createAll(
          node.children,
          Some(newId),
          isTrash,
          project,
          propertyResolver,
          documentIdMap,
          pending
        )
      }
    }

  private[this] def postCreate(
      oldDocumentId: String,
      newDocumentId: String,
      document: BacklogDocument,
      propertyResolver: PropertyResolver,
      pending: mutable.ArrayBuffer[PendingDocument]
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[Unit] =
    for {
      _ <- ConsoleDSL[Task].println(s"[Document id=$newDocumentId]")
      _ <- logStep("Document created(title, emoji)", ok = true)
      // Comments must be created before the content update: the body's
      // inlineComment marks reference comment ids, which only exist once
      // comments have been (re-)created at the destination.
      commentIdMap <- postComments(newDocumentId, document, propertyResolver)
      _            <- logStepCount("Comments imported", commentIdMap.size, document.comments.size)
      withRewrittenComments <- Task(
        documentService.rewriteInlineCommentIds(document, commentIdMap)
      )
      tagsResult        <- postTags(newDocumentId, document)
      _                 <- logStepCount("Tags added", tagsResult._1, tagsResult._2)
      attachmentsResult <- postAttachments(oldDocumentId, newDocumentId, document)
      _ <- logStepCount(
        "Attachments added",
        attachmentsResult._1,
        attachmentsResult._2
      )
      _ <- Task(pending += PendingDocument(oldDocumentId, newDocumentId, withRewrittenComments))
    } yield ()

  private[this] def finalizeContent(
      pendingDocument: PendingDocument,
      ctx: MentionRewriteContext,
      documentIdMap: Map[String, String],
      propertyResolver: PropertyResolver
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[Unit] =
    for {
      _ <- ConsoleDSL[Task].println(s"[Document id=${pendingDocument.newDocumentId}]")
      mentionRewriteResult <- Task {
        documentService.rewriteMentions(
          pendingDocument.document,
          ctx.issueIdMap,
          ctx.issueKeyMap,
          documentIdMap,
          ctx.srcProjectId,
          ctx.srcProjectKey,
          ctx.dstProjectId,
          ctx.dstProjectKey
        )
      }
      _ <- logStep("Document content rewritten", ok = true)
      _ <- logIssueMentionStep(mentionRewriteResult._2)
      _ <- logDocumentMentionStep(mentionRewriteResult._3)
      _ <- Task(
        documentService.updateContent(
          pendingDocument.newDocumentId,
          mentionRewriteResult._1,
          propertyResolver
        )
      )
      _ <- logStep("Document content updated", ok = true)
    } yield ()

  // Always OK: reaching this call means the step didn't throw.
  private[this] def logStep(label: String, ok: Boolean)(implicit
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] =
    if (ok) ConsoleDSL[Task].println(s"$label: OK", space = 2, color = GREEN)
    else ConsoleDSL[Task].errorln(s"$label: NG", space = 2)

  // e.g. "Comments imported: OK (3)" or "Attachments added: NG (1/2)".
  private[this] def logStepCount(label: String, success: Int, total: Int)(implicit
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] =
    if (success == total)
      ConsoleDSL[Task].println(s"$label: OK ($total)", space = 2, color = GREEN)
    else ConsoleDSL[Task].errorln(s"$label: NG ($success/$total)", space = 2)

  // Skipped mentions aren't failures; only unresolved ones make this NG.
  private[this] def logIssueMentionStep(stats: IssueMentionRewriteStats)(implicit
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] = {
    val details = Seq(
      Option.when(stats.skippedExternalProject > 0)(s"${stats.skippedExternalProject} skipped"),
      Option.when(stats.unresolved > 0)(s"${stats.unresolved} unresolved")
    ).flatten
    val suffix    = if (details.isEmpty) "" else s", ${details.mkString(", ")}"
    val countText = s"${stats.rewritten}/${stats.total}$suffix"
    if (stats.unresolved == 0)
      ConsoleDSL[Task].println(
        s"Issue mentions rewritten: OK ($countText)",
        space = 2,
        color = GREEN
      )
    else
      ConsoleDSL[Task].errorln(s"Issue mentions rewritten: NG ($countText)", space = 2)
  }

  // Skipped mentions aren't failures; only unresolved ones make this NG.
  private[this] def logDocumentMentionStep(stats: DocumentMentionRewriteStats)(implicit
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] = {
    val details = Seq(
      Option.when(stats.skippedExternalProject > 0)(s"${stats.skippedExternalProject} skipped"),
      Option.when(stats.unresolved > 0)(s"${stats.unresolved} unresolved")
    ).flatten
    val suffix    = if (details.isEmpty) "" else s", ${details.mkString(", ")}"
    val countText = s"${stats.rewritten}/${stats.total}$suffix"
    if (stats.unresolved == 0)
      ConsoleDSL[Task].println(
        s"Document mentions rewritten: OK ($countText)",
        space = 2,
        color = GREEN
      )
    else
      ConsoleDSL[Task].errorln(s"Document mentions rewritten: NG ($countText)", space = 2)
  }

  private[this] def postAttachments(
      oldDocumentId: String,
      newDocumentId: String,
      document: BacklogDocument
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[(Int, Int)] = {
    val total = document.attachments.size
    Task
      .sequence(document.attachments.map { attachment =>
        toPath(oldDocumentId, attachment) match {
          case Some(path) =>
            documentService.addAttachment(newDocumentId, path.pathAsString) match {
              case Right(_) => Task(true)
              case Left(e) =>
                ConsoleDSL[Task]
                  .errorln(
                    Messages("import.error.document.attachment", attachment.name, e.getMessage)
                  )
                  .map(_ => false)
            }
          case None =>
            logger.warn(s"${attachment.name} does not exist")
            Task(false)
        }
      })
      .map(results => (results.count(identity), total))
  }

  private[this] def toPath(oldDocumentId: String, attachment: BacklogAttachment): Option[Path] =
    attachment.optId
      .map(id => backlogPaths.documentAttachmentPath(oldDocumentId, s"${id}_${attachment.name}"))
      .filter(_.exists)

  private[this] def postTags(
      newDocumentId: String,
      document: BacklogDocument
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[(Int, Int)] = {
    val tagNames = document.tags.map(_.name)
    val total    = tagNames.size
    if (tagNames.isEmpty) Task((0, 0))
    else
      documentService.addTags(newDocumentId, tagNames) match {
        case Right(_) => Task((total, total))
        case Left(e) =>
          ConsoleDSL[Task]
            .errorln(Messages("import.error.document.tags", document.title, e.getMessage))
            .map(_ => (0, total))
      }
  }

  // Returns a map of old (source-space) comment id -> new (destination-space)
  // comment id, so the document body's inlineComment marks can be rewritten
  // to point at the newly created comments.
  private[this] def postComments(
      newDocumentId: String,
      document: BacklogDocument,
      propertyResolver: PropertyResolver
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[Map[String, String]] =
    Task
      .sequence(document.comments.map { comment =>
        documentService.addComment(newDocumentId, comment, propertyResolver) match {
          case Right(newCommentId) =>
            Task(comment.optId.map(oldCommentId => oldCommentId -> newCommentId))
          case Left(e) =>
            ConsoleDSL[Task]
              .errorln(
                Messages("import.error.document.comment", document.title, e.getMessage)
              )
              .map(_ => None)
        }
      })
      .map(_.flatten.toMap)

  private[this] def unmarshal(documentId: String): Option[BacklogDocument] =
    BacklogUnmarshaller.document(backlogPaths.documentJson(documentId))

}
