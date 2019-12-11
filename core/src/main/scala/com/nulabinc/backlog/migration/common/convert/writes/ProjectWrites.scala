package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogProject
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.Project

/**
  * @author uchida
  */
private[common] class ProjectWrites @Inject()() extends Writes[Project, BacklogProject] with Logging {

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
