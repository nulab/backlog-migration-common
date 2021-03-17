package com.nulabinc.backlog.migration.importer.service

import javax.inject.Inject
import cats.Monad
import cats.syntax.all._
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.convert.BacklogUnmarshaller
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.domain.exports.{
  DeletedExportedBacklogStatus,
  ExistingExportedBacklogStatus
}
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.service.{PropertyResolver, _}
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.osinka.i18n.Messages
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import monix.eval.Task
import monix.execution.Scheduler

/**
 * @author uchida
 */
private[importer] class ProjectImporter @Inject() (
    attachmentService: AttachmentService,
    backlogPaths: BacklogPaths,
    commentService: CommentService,
    groupService: GroupService,
    projectService: ProjectService,
    versionService: VersionService,
    projectUserService: ProjectUserService,
    issueService: IssueService,
    issueTypeService: IssueTypeService,
    issueCategoryService: IssueCategoryService,
    customFieldSettingService: CustomFieldSettingService,
    wikisImporter: WikisImporter,
    resolutionService: ResolutionService,
    userService: UserService,
    sharedFileService: SharedFileService,
    statusService: StatusService,
    priorityService: PriorityService
) extends Logging {

  def execute[A](
      fitIssueKey: Boolean,
      retryCount: Int
  )(implicit s: Scheduler, storeDSL: StoreDSL[Task], consoleDSL: ConsoleDSL[Task]): Task[Unit] = {
    val project = BacklogUnmarshaller.project(backlogPaths)
    projectService.create(project) match {
      case Right(project) =>
        for {
          _ <- preExecute()
          _ <- contents(project, fitIssueKey, retryCount)
          _ <- postExecute()
          _ <- ConsoleDSL[Task].printStream(
            ansi.cursorLeft(999).cursorUp(1).eraseLine(Ansi.Erase.ALL)
          )
          _ <- ConsoleDSL[Task].printStream(
            ansi.cursorLeft(999).cursorUp(1).eraseLine(Ansi.Erase.ALL)
          )
          _ <- ConsoleDSL[Task].flush()
          _ <- ConsoleDSL[Task].println(ConsoleMessages.Imports.finish)
        } yield ()
      case Left(e) =>
        import ConsoleMessages.Imports._

        val message =
          if (e.getMessage.contains("Project limit."))
            Errors.limitProject(project.key)
          else if (e.getMessage.contains("Duplicate entry"))
            Errors.projectNotJoin(project.key)
          else {
            logger.error(e.getMessage, e)
            Errors.failed(project.key, e.getMessage())
          }
        for {
          _ <- ConsoleDSL[Task].errorln(message)
          _ <- ConsoleDSL[Task].println(Errors.suspend)
        } yield ()
    }
  }

  private def contents(
      project: BacklogProject,
      fitIssueKey: Boolean,
      retryCount: Int
  )(implicit s: Scheduler, storeDSL: StoreDSL[Task], consoleDSL: ConsoleDSL[Task]): Task[Unit] = {
    val issuesImporter = new IssuesImporter(
      backlogPaths = backlogPaths,
      sharedFileService = sharedFileService,
      issueService = issueService,
      commentService = commentService,
      attachmentService = attachmentService
    )
    val propertyResolver = buildPropertyResolver()

    //Wiki
    wikisImporter.execute(project.id, propertyResolver)

    //Issue
    issuesImporter.execute(project, propertyResolver, fitIssueKey, retryCount)
  }

  private def preExecute()(implicit
      storeDSL: StoreDSL[Task],
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] = {
    val propertyResolver = buildPropertyResolver()
    importGroup(propertyResolver)
    importProjectUser(propertyResolver)
    importVersion()
    importCategory()
    importIssueType()
    importCustomField()

    for {
      _ <- importStatuses()
    } yield ()
  }

  private def postExecute()(implicit
      storeDSL: StoreDSL[Task],
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] = {
    val propertyResolver = buildPropertyResolver()

    removeVersion(propertyResolver)
    removeCategory(propertyResolver)
    removeCustomField(propertyResolver)

    for {
      _ <- removeStatus(propertyResolver)
    } yield {
      BacklogUnmarshaller.backlogCustomFieldSettings(backlogPaths).filter(!_.delete).foreach {
        customFieldSetting =>
          customFieldSettingService.update(
            customFieldSettingService.setUpdateParams(propertyResolver)
          )(customFieldSetting)
      }
    }
  }

  private[this] def importGroup(propertyResolver: PropertyResolver): Unit = {
    val groups = groupService.allGroups()
    def exists(group: BacklogGroup): Boolean = {
      groups.exists(_.name == group.name)
    }
    val backlogGroups =
      BacklogUnmarshaller.groups(backlogPaths).filterNot(exists)
    val console = (ProgressBar.progress _)(
      Messages("common.groups"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    backlogGroups.zipWithIndex.foreach {
      case (backlogGroup, index) =>
        groupService.create(backlogGroup, propertyResolver)
        console(index + 1, backlogGroups.size)
    }
  }

  private[this] def importVersion(): Unit = {
    val versions = versionService.allVersions()
    def exists(version: BacklogVersion): Boolean = {
      versions.exists(_.name == version.name)
    }
    val backlogVersions =
      BacklogUnmarshaller.versions(backlogPaths).filterNot(exists)
    val console = (ProgressBar.progress _)(
      Messages("common.version"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    backlogVersions.zipWithIndex.foreach {
      case (backlogVersion, index) =>
        versionService.add(backlogVersion)
        console(index + 1, backlogVersions.size)
    }
  }

  private[this] def importCategory(): Unit = {
    val categories = issueCategoryService.allIssueCategories()
    def exists(issueCategory: BacklogIssueCategory): Boolean = {
      categories.exists(_.name == issueCategory.name)
    }
    val issueCategories =
      BacklogUnmarshaller.issueCategories(backlogPaths).filterNot(exists)
    val console = (ProgressBar.progress _)(
      Messages("common.category"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    issueCategories.zipWithIndex.foreach {
      case (issueCategory, index) =>
        issueCategoryService.add(issueCategory)
        console(index + 1, issueCategories.size)
    }
  }

  private[this] def importIssueType(): Unit = {
    val issueTypes = issueTypeService.allIssueTypes()
    def exists(issueType: BacklogIssueType): Boolean = {
      issueTypes.exists(_.name == issueType.name)
    }
    val backlogIssueTypes =
      BacklogUnmarshaller.issueTypes(backlogPaths).filterNot(exists)
    val console = (ProgressBar.progress _)(
      Messages("common.issue_type"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    backlogIssueTypes.zipWithIndex.foreach {
      case (backlogIssueType, index) =>
        issueTypeService.add(backlogIssueType)
        console(index + 1, backlogIssueTypes.size)
    }
  }

  private def importStatuses()(implicit
      storeDSL: StoreDSL[Task],
      consoleDSL: ConsoleDSL[Task]
  ): Task[Unit] = {
    val projectStatuses = statusService.allStatuses()

    for {
      willExistDestinationStatuses <- StoreDSL[Task].allSrcStatus
    } yield {
      // Import Statuses excluding default statuses
      val mustImportCustomStatuses = willExistDestinationStatuses
        .filter {
          case s: ExistingExportedBacklogStatus =>
            projectStatuses.isCustomStatus(s.status) && projectStatuses.notExistByName(s.name)
          case s: DeletedExportedBacklogStatus =>
            projectStatuses.notExistByName(s.name)
        }
        .flatMap {
          case backlogStatus: ExistingExportedBacklogStatus =>
            backlogStatus.status match {
              case _: BacklogDefaultStatus => None
              case s: BacklogCustomStatus  => Some(s)
            }
          case s: DeletedExportedBacklogStatus =>
            Some(BacklogCustomStatus.create(s.name))
        }
      val console = (ProgressBar.progress _)(
        Messages("common.statuses"),
        Messages("message.importing"),
        Messages("message.imported")
      )

      val importedCustomStatuses = mustImportCustomStatuses.zipWithIndex.map {
        case (exportedStatus, index) =>
          val added = statusService.add(exportedStatus)
          console(index + 1, mustImportCustomStatuses.size)
          added.copy(displayOrder =
            exportedStatus.displayOrder
          ) // Added display order is always 3999. Must update from old one.
      }

      // Update display orders
      val updatedAllDestinationStatusIds =
        projectStatuses
          .append(importedCustomStatuses)
          .sortBy(_.displayOrder)
          .map(_.id)

      statusService.updateOrder(updatedAllDestinationStatusIds)
    }
  }

  private[this] def importProjectUser(
      propertyResolver: PropertyResolver
  ): Unit = {
    val projectUsers = BacklogUnmarshaller.projectUsers(backlogPaths)
    val console = (ProgressBar.progress _)(
      Messages("common.project_user"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    projectUsers.zipWithIndex.foreach {
      case (projectUser, index) =>
        for {
          userId <- projectUser.optUserId
          id     <- propertyResolver.optResolvedUserId(userId)
        } yield projectUserService.add(id)
        console(index + 1, projectUsers.size)
    }
  }

  private[this] def importCustomField(): Unit = {
    val customFieldSettings = customFieldSettingService.allCustomFieldSettings()
    val backlogCustomFields = customFieldSettings.filterNotExist(
      BacklogUnmarshaller.backlogCustomFieldSettings(backlogPaths)
    )
    val console = (ProgressBar.progress _)(
      Messages("common.custom_field"),
      Messages("message.importing"),
      Messages("message.imported")
    )
    backlogCustomFields.zipWithIndex.foreach {
      case (backlogCustomField, index) =>
        customFieldSettingService.add(customFieldSettingService.setAddParams)(
          backlogCustomField
        )
        console(index + 1, backlogCustomFields.size)
    }
  }

  private[this] def removeVersion(propertyResolver: PropertyResolver): Unit = {
    BacklogUnmarshaller.versions(backlogPaths).filter(_.delete).foreach { version =>
      for {
        versionId <- propertyResolver.optResolvedVersionId(version.name)
      } yield {
        try {
          versionService.remove(versionId)
        } catch {
          case ex: Throwable =>
            logger.warn(
              s"Remove version [${version.name}] failed. ${ex.getMessage}"
            )
        }
      }
    }
  }

  private[this] def removeCategory(propertyResolver: PropertyResolver): Unit = {
    BacklogUnmarshaller.issueCategories(backlogPaths).filter(_.delete).foreach { category =>
      for {
        issueCategoryId <- propertyResolver.optResolvedCategoryId(category.name)
      } yield {
        try {
          issueCategoryService.remove(issueCategoryId)
        } catch {
          case ex: Throwable =>
            logger.warn(
              s"Remove category [${category.name}] failed. ${ex.getMessage}"
            )
        }
      }
    }
  }

  private[this] def removeCustomField(
      propertyResolver: PropertyResolver
  ): Unit =
    BacklogUnmarshaller.backlogCustomFieldSettings(backlogPaths).filter(_.delete).foreach {
      backlogCustomFieldSetting =>
        for {
          targetCustomFieldSetting <- propertyResolver.optResolvedCustomFieldSetting(
            backlogCustomFieldSetting.name
          )
          customFieldSettingId <- targetCustomFieldSetting.optId
        } yield {
          try {
            customFieldSettingService.remove(customFieldSettingId)
          } catch {
            case ex: Throwable =>
              logger.warn(
                s"Remove custom field [${backlogCustomFieldSetting.name}] failed. ${ex.getMessage}"
              )
          }
        }
    }

  private def removeStatus(
      propertyResolver: PropertyResolver
  )(implicit storeDSL: StoreDSL[Task], consoleDSL: ConsoleDSL[Task]): Task[Unit] =
    for {
      exported <- StoreDSL[Task].allSrcStatus
    } yield {
      exported
        .flatMap {
          case s: DeletedExportedBacklogStatus  => Some(s.name)
          case _: ExistingExportedBacklogStatus => None
        }
        .foreach { name =>
          val statusId = propertyResolver.tryResolvedStatusId(name)

          try {
            statusService.remove(statusId)
          } catch {
            case ex: Throwable =>
              logger.warn(
                s"Remove status [${name.trimmed}] failed. ${ex.getMessage}"
              )
          }
        }
    }

  private[this] def buildPropertyResolver(): PropertyResolver =
    new PropertyResolverImpl(
      customFieldSettingService,
      issueTypeService,
      issueCategoryService,
      versionService,
      resolutionService,
      userService,
      statusService,
      priorityService
    )

}
