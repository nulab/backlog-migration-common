package com.nulabinc.backlog.migration.common.modules

import com.google.inject.AbstractModule
import com.nulabinc.backlog.migration.common.client.{
  BacklogAPIClient,
  BacklogAPIClientImpl
}
import com.nulabinc.backlog.migration.common.conf.{
  BacklogApiConfiguration,
  BacklogPaths
}
import com.nulabinc.backlog.migration.common.domain.{
  BacklogProjectKey,
  PropertyValue
}
import com.nulabinc.backlog.migration.common.service._
import com.nulabinc.backlog4j.conf.BacklogPackageConfigure
import com.nulabinc.backlog4j.IssueType

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class DefaultModule(apiConfig: BacklogApiConfiguration) extends AbstractModule {

  protected val backlog: BacklogAPIClient = createBacklogAPIClient()

  override def configure(): Unit = {
    //base
    bind(classOf[BacklogAPIClient]).toInstance(backlog)
    bind(classOf[BacklogProjectKey])
      .toInstance(BacklogProjectKey(apiConfig.projectKey))
    bind(classOf[BacklogPaths]).toInstance(
      new BacklogPaths(apiConfig.projectKey, apiConfig.backlogOutputPath)
    )
    bind(classOf[PropertyValue]).toInstance(createPropertyValue())

    bind(classOf[CommentService]).to(classOf[CommentServiceImpl])
    bind(classOf[CustomFieldSettingService])
      .to(classOf[CustomFieldSettingServiceImpl])
    bind(classOf[GroupService]).to(classOf[GroupServiceImpl])
    bind(classOf[IssueCategoryService]).to(classOf[IssueCategoryServiceImpl])
    bind(classOf[IssueService]).to(classOf[IssueServiceImpl])
    bind(classOf[IssueTypeService]).to(classOf[IssueTypeServiceImpl])
    bind(classOf[ProjectService]).to(classOf[ProjectServiceImpl])
    bind(classOf[ProjectUserService]).to(classOf[ProjectUserServiceImpl])
    bind(classOf[SharedFileService]).to(classOf[SharedFileServiceImpl])
    bind(classOf[VersionService]).to(classOf[VersionServiceImpl])
    bind(classOf[WikiService]).to(classOf[WikiServiceImpl])
    bind(classOf[ResolutionService]).to(classOf[ResolutionServiceImpl])
    bind(classOf[StatusService]).to(classOf[StatusServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    bind(classOf[PriorityService]).to(classOf[PriorityServiceImpl])
    bind(classOf[SpaceService]).to(classOf[SpaceServiceImpl])
    bind(classOf[AttachmentService]).to(classOf[AttachmentServiceImpl])
  }

  private[this] def createBacklogAPIClient(): BacklogAPIClient = {
    val backlogPackageConfigure = new BacklogPackageConfigure(apiConfig.url)
    val configure = backlogPackageConfigure.apiKey(apiConfig.key)

    new BacklogAPIClientImpl(configure)
  }

  private[this] def createPropertyValue(): PropertyValue = {
    val issueTypes =
      try {
        backlog.getIssueTypes(apiConfig.projectKey).asScala.toSeq
      } catch {
        case _: Throwable => Seq.empty[IssueType]
      }
    PropertyValue(issueTypes)
  }
}
