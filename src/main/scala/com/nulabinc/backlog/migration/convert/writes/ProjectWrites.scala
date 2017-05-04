package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogProject
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.Project

/**
  * @author uchida
  */
class ProjectWrites @Inject()() extends Writes[Project, BacklogProject] with Logging {

  override def writes(project: Project): BacklogProject = {
    BacklogProject(
      optId = Some(project.getId),
      name = project.getName,
      key = project.getProjectKey,
      isChartEnabled = project.isChartEnabled,
      isSubtaskingEnabled = project.isSubtaskingEnabled,
      textFormattingRule = project.getTextFormattingRule.getStrValue
    )
  }

}
