package com.nulabinc.backlog.migration.importer.service

import better.files.{File => Path}
import cats.Monad
import cats.syntax.all._
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.convert.BacklogUnmarshaller
import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogComment,
  BacklogIssue,
  BacklogProject
}
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.service._
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, IssueKeyUtil, Logging, _}
import com.nulabinc.backlog.migration.importer.core.RetryException
import com.nulabinc.backlog4j.BacklogAPIException
import com.osinka.i18n.Messages

import javax.inject.Inject

/**
 * @author uchida
 */
private[importer] class IssuesImporter @Inject() (
    backlogPaths: BacklogPaths,
    sharedFileService: SharedFileService,
    issueService: IssueService,
    commentService: CommentService,
    attachmentService: AttachmentService
) extends Logging {

  import com.nulabinc.backlog.migration.importer.core.RetryUtil._

  private[this] val console       = new IssueProgressBar()
  private[this] val retryInterval = 5000

  def execute[F[_]: Monad: ConsoleDSL](
      project: BacklogProject,
      propertyResolver: PropertyResolver,
      fitIssueKey: Boolean,
      retryCount: Int
  ): F[Unit] = {

    for {
      _ <- ConsoleDSL[F].println("""
      """.stripMargin)
    } yield {
      console.totalSize = totalSize()

      implicit val context =
        IssueContext(project, propertyResolver, fitIssueKey, retryCount)
      val paths = IOUtil.directoryPaths(backlogPaths.issueDirectoryPath).sortWith(_.name < _.name)
      paths.zipWithIndex.foreach {
        case (path, index) =>
          loadDateDirectory(path, index)
      }
    }

  }

  private[this] def loadDateDirectory(path: Path, index: Int)(implicit
      ctx: IssueContext
  ): Unit = {
    val jsonDirs =
      path.list.filter(_.isDirectory).toSeq.sortWith(compareIssueJsons)
    console.date = DateUtil.yyyymmddToSlashFormat(path.name)
    console.failed = 0

    jsonDirs.zipWithIndex.foreach {
      case (jsonDir, index) =>
        loadJson(jsonDir, index, jsonDirs.size)
    }
  }

  private[this] def loadJson(path: Path, index: Int, size: Int)(implicit
      ctx: IssueContext
  ): Unit = {
    BacklogUnmarshaller.issue(backlogPaths.issueJson(path)) match {
      case Some(issue: BacklogIssue) =>
        retryBacklogAPIException(ctx.retryCount, retryInterval) {
          // BLGMIGRATION-936
          createTemporaryIssues(issue, index, size)
        }
        retryBacklogAPIException(ctx.retryCount, retryInterval) {
          createIssue(issue, path, index, size)
        }
      case Some(comment: BacklogComment) =>
        createComment(comment, path, index, size)
      case _ =>
        None
    }
    console.count = console.count + 1
  }

  private def createIssue(
      issue: BacklogIssue,
      path: Path,
      index: Int,
      size: Int
  )(implicit ctx: IssueContext): Unit = {
    val prevSuccessIssueId = ctx.optPrevIssueIndex

    if (issueService.exists(ctx.project.id, issue)) {
      ctx.excludeIssueIds += issue.id
      for {
        remoteIssue <- issueService.optIssueOfParams(ctx.project.id, issue)
      } yield {
        ctx.addIssueId(issue, remoteIssue)
      }
      console.warning(
        index + 1,
        size,
        Messages(
          "import.issue.already_exists",
          issue.optIssueKey.getOrElse(issue.id.toString)
        )
      )
    } else {
      issueService.create(
        issueService.setCreateParam(
          ctx.project.id,
          ctx.propertyResolver,
          ctx.toRemoteIssueId,
          postAttachment(path, index, size),
          issueService.issueOfId
        )
      )(issue) match {
        case Right(remoteIssue) =>
          sharedFileService.linkIssueSharedFile(remoteIssue.id, issue)
          ctx.addIssueId(issue, remoteIssue)
        case _ =>
          ctx.optPrevIssueIndex = prevSuccessIssueId
          console.failed += 1
      }
      console.progress(index + 1, size)
    }
  }

  private def createTemporaryIssues(
      issue: BacklogIssue,
      index: Int,
      size: Int
  )(implicit ctx: IssueContext): Unit = {
    val optIssueIndex  = issue.optIssueKey.map(IssueKeyUtil.findIssueIndex)
    val prevIssueIndex = ctx.optPrevIssueIndex.getOrElse(0)

    for {
      issueIndex <- optIssueIndex
      if (prevIssueIndex + 1) != issueIndex
      if ctx.fitIssueKey
    } yield {
      (prevIssueIndex + 1) until issueIndex
    }.foreach { dummyIndex =>
      createTemporaryIssue(dummyIndex, index, size)
    }
    ctx.optPrevIssueIndex = optIssueIndex
  }

  private def createTemporaryIssue(dummyIndex: Int, index: Int, size: Int)(implicit
      ctx: IssueContext
  ) = {
    val dummyIssue =
      issueService.createDummy(ctx.project.id, ctx.propertyResolver)
    issueService.delete(dummyIssue.getId)
    logger.warn(
      s"${Messages("import.issue.create_dummy", s"${ctx.project.key}-${dummyIndex}")}"
    )
  }

  private[this] def createComment(
      comment: BacklogComment,
      path: Path,
      index: Int,
      size: Int
  )(implicit ctx: IssueContext) = {

    def updateComment(remoteIssueId: Long): Unit = {

      val setUpdatedParam =
        retryBacklogAPIException(ctx.retryCount, retryInterval) {
          commentService.setUpdateParam(
            remoteIssueId,
            ctx.propertyResolver,
            ctx.toRemoteIssueId,
            postAttachment(path, index, size)
          ) _
        }

      try {
        retryBacklogAPIException(ctx.retryCount, retryInterval) {
          commentService.update(setUpdatedParam)(comment) match {
            case Left(e)
                if Option(e.getMessage)
                  .getOrElse("")
                  .contains("Please change the status or post a comment.") =>
              logger.warn(e.getMessage, e)
            case Left(e) =>
              throw e
            case Right(_) =>
              ()
          }
        }
      } catch {
        case e: RetryException =>
          handleUpdateCommentError(e, remoteIssueId)
        case e: BacklogAPIException =>
          handleUpdateCommentError(e, remoteIssueId)
        case e: Throwable =>
          throw e
      }
    }

    def handleUpdateCommentError(e: Throwable, remoteIssueId: Long): Unit = {
      logger.error(e.getMessage, e)
      val issue = issueService.issueOfId(remoteIssueId)
      console.error(
        index + 1,
        size,
        s"${Messages("import.error.failed.comment", issue.optIssueKey.getOrElse(issue.id.toString), e.getMessage)}"
      )
      console.failed += 1
    }

    def deleteAttachment(remoteIssueId: Long) =
      comment.changeLogs.filter { _.mustDeleteAttachment }.map { changeLog =>
        val issueAttachments =
          attachmentService.allAttachmentsOfIssue(remoteIssueId) match {
            case Right(attachments) => attachments
            case Left(_)            => Seq.empty[BacklogAttachment]
          }
        for {
          attachmentInfo      <- changeLog.optAttachmentInfo
          attachment          <- issueAttachments.sortBy(_.optId).find(_.name == attachmentInfo.name)
          attachmentId        <- attachment.optId
          createdUser         <- comment.optCreatedUser
          createdUserId       <- createdUser.optUserId
          solvedCreatedUserId <- ctx.propertyResolver.optResolvedUserId(createdUserId)
          created             <- comment.optCreated
        } yield {
          issueService.deleteAttachment(
            remoteIssueId,
            attachmentId,
            solvedCreatedUserId,
            created
          )
        }
      }

    for {
      issueId       <- comment.optIssueId
      remoteIssueId <- ctx.toRemoteIssueId(issueId)
    } yield {
      if (!ctx.excludeIssueIds.contains(issueId)) {
        if (comment.changeLogs.exists(_.mustDeleteAttachment)) {
          deleteAttachment(remoteIssueId)
        } else {
          updateComment(remoteIssueId)
        }
        console.progress(index + 1, size)
      }
    }
  }

  private[this] val postAttachment = (path: Path, index: Int, size: Int) => {
    fileName: String =>
      {
        val files = backlogPaths.issueAttachmentDirectoryPath(path).list
        files.find(file => file.name == fileName) match {
          case Some(filePath) =>
            attachmentService.postAttachment(filePath.pathAsString) match {
              case Right(attachment) => attachment.optId
              case Left(e) =>
                if (
                  e.getMessage.indexOf(
                    "The size of attached file is too large."
                  ) >= 0
                )
                  console.error(
                    index + 1,
                    size,
                    Messages("import.error.attachment.too_large", filePath.name)
                  )
                else
                  console.error(
                    index + 1,
                    size,
                    Messages(
                      "import.error.issue.attachment",
                      filePath.name,
                      e.getMessage
                    )
                  )
                None
            }
          case _ => None

        }
      }: Option[Long]
  }

  private[this] def compareIssueJsons(path1: Path, path2: Path): Boolean = {
    def getTimestamp(value: String): Long = value.split("-")(0).toLong

    def getIssueId(value: String): Long = value.split("-")(1).toLong

    def getType(value: String) = value.split("-")(2)

    def getIndex(value: String) = value.split("-")(3).toInt

    if (getTimestamp(path1.name) == getTimestamp(path2.name)) {
      if (getType(path1.name) == getType(path2.name))
        if (getIssueId(path1.name) == getIssueId(path2.name))
          getIndex(path1.name) < getIndex(path2.name)
        else getIssueId(path1.name) < getIssueId(path2.name)
      else getType(path1.name) > getType(path2.name)
    } else getTimestamp(path1.name) < getTimestamp(path2.name)
  }

  private[this] def totalSize(): Int = {
    val paths = IOUtil.directoryPaths(backlogPaths.issueDirectoryPath)
    paths.foldLeft(0) { (count, path) =>
      count + path.list.size
    }
  }

}
