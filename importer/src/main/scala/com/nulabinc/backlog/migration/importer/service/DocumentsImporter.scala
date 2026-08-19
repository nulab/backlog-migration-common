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
import com.nulabinc.backlog.migration.common.service.{DocumentService, PropertyResolver}
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.osinka.i18n.Messages
import monix.eval.Task
import monix.execution.Scheduler

/**
 * @author
 *   nulab
 */
private[importer] class DocumentsImporter @Inject() (
    backlogPaths: BacklogPaths,
    documentService: DocumentService
) extends Logging {

  def execute(project: BacklogProject, propertyResolver: PropertyResolver)(implicit
      s: Scheduler,
      consoleDSL: ConsoleDSL[Task]
  ): Unit =
    BacklogUnmarshaller.documentTree(backlogPaths).foreach { tree =>
      val total = countNodes(tree.activeTree.children)
      val console = (ProgressBar.progress _)(
        Messages("common.documents"),
        Messages("message.importing"),
        Messages("message.imported")
      )
      var done = 0

      def progress(): Unit = {
        done += 1
        console(done, total)
      }

      // Only the active tree is migrated; trashed documents are not restored.
      walk(tree.activeTree.children, None, project, propertyResolver, progress)
    }

  private[this] def countNodes(nodes: Seq[BacklogDocumentTreeNode]): Int =
    nodes.map(node => 1 + countNodes(node.children)).sum

  private[this] def walk(
      nodes: Seq[BacklogDocumentTreeNode],
      optNewParentId: Option[String],
      project: BacklogProject,
      propertyResolver: PropertyResolver,
      progress: () => Unit
  )(implicit s: Scheduler, consoleDSL: ConsoleDSL[Task]): Unit =
    nodes.foreach { node =>
      val optNewId = unmarshal(node.id).map { document =>
        val newId = documentService.create(
          project.id,
          document,
          optNewParentId,
          addLast = true,
          propertyResolver
        )
        postCreate(node.id, newId, document, propertyResolver).runSyncUnsafe()
        newId
      }
      progress()
      // A failed/missing parent breaks the id mapping, so its children are skipped too.
      optNewId.foreach { newId =>
        walk(node.children, Some(newId), project, propertyResolver, progress)
      }
    }

  private[this] def postCreate(
      oldDocumentId: String,
      newDocumentId: String,
      document: BacklogDocument,
      propertyResolver: PropertyResolver
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[Unit] =
    for {
      // Comments must be created before the content update: the body's
      // inlineComment marks reference comment ids, which only exist once
      // comments have been (re-)created at the destination.
      commentIdMap <- postComments(newDocumentId, document, propertyResolver)
      _ <- Task(
        documentService.updateContent(
          newDocumentId,
          documentService.rewriteInlineCommentIds(document, commentIdMap),
          propertyResolver
        )
      )
      _ <- postAttachments(oldDocumentId, newDocumentId, document)
      _ <- postTags(newDocumentId, document)
    } yield ()

  private[this] def postAttachments(
      oldDocumentId: String,
      newDocumentId: String,
      document: BacklogDocument
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[Unit] =
    Task
      .sequence(document.attachments.map { attachment =>
        toPath(oldDocumentId, attachment) match {
          case Some(path) =>
            documentService.addAttachment(newDocumentId, path.pathAsString) match {
              case Right(_) => Task.unit
              case Left(e) =>
                ConsoleDSL[Task].errorln(
                  Messages("import.error.document.attachment", attachment.name, e.getMessage)
                )
            }
          case None =>
            logger.warn(s"${attachment.name} does not exist")
            Task.unit
        }
      })
      .map(_ => ())

  private[this] def toPath(oldDocumentId: String, attachment: BacklogAttachment): Option[Path] =
    attachment.optId
      .map(id => backlogPaths.documentAttachmentPath(oldDocumentId, s"${id}_${attachment.name}"))
      .filter(_.exists)

  private[this] def postTags(
      newDocumentId: String,
      document: BacklogDocument
  )(implicit consoleDSL: ConsoleDSL[Task]): Task[Unit] = {
    val tagNames = document.tags.map(_.name)
    if (tagNames.isEmpty) Task.unit
    else
      documentService.addTags(newDocumentId, tagNames) match {
        case Right(_) => Task.unit
        case Left(e) =>
          ConsoleDSL[Task].errorln(
            Messages("import.error.document.tags", document.title, e.getMessage)
          )
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
