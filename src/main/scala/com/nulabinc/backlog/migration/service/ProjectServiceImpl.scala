package com.nulabinc.backlog.migration.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.Backlog4jConverters
import com.nulabinc.backlog.migration.domain.BacklogProject
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.api.option.CreateProjectParams
import com.nulabinc.backlog4j.{BacklogAPIException, BacklogClient, Project}

/**
  * @author uchida
  */
class ProjectServiceImpl @Inject()(backlog: BacklogClient) extends ProjectService with Logging {

  override def create(project: BacklogProject): Either[Throwable, BacklogProject] =
    optProject(project.key) match {
      case Some(project) => Right(project)
      case _             => doCreate(project)
    }

  override def optProject(projectKey: String): Option[BacklogProject] =
    try {
      Some(Backlog4jConverters.Project(backlog.getProject(projectKey)))
    } catch {
      case e: BacklogAPIException =>
        if (!(e.getMessage.contains("No project") || e.getMessage.contains("No such project"))) {
          logger.error(e.getMessage, e)
        }
        None
    }

  private[this] def doCreate(project: BacklogProject): Either[Throwable, BacklogProject] = {
    val params = new CreateProjectParams(
      project.name,
      project.key,
      project.isChartEnabled,
      project.isSubtaskingEnabled,
      Project.TextFormattingRule.enumValueOf(project.textFormattingRule)
    )
    try {
      Right(Backlog4jConverters.Project(backlog.createProject(params)))
    } catch {
      case e: Throwable =>
        Left(e)
    }
  }

  override def projectOfKey(projectKey: String): BacklogProject =
    Backlog4jConverters.Project(backlog.getProject(projectKey))

}
