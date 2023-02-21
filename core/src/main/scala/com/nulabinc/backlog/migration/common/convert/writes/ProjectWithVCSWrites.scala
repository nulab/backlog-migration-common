package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogProject
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.ProjectWithVCS

/**
 * @author
 *   uchida
 */
private[common] class ProjectWithVCSWrites @Inject() ()
    extends Writes[ProjectWithVCS, BacklogProject]
    with Logging {

  override def writes(project: ProjectWithVCS): BacklogProject = {
    BacklogProject(
      optId = Some(project.getId),
      name = project.getName,
      key = project.getProjectKey,
      isChartEnabled = project.isChartEnabled,
      isSubtaskingEnabled = project.isSubtaskingEnabled,
      textFormattingRule = project.getTextFormattingRule.getStrValue,
      isProjectLeaderCanEditProjectLeader = project.isProjectLeaderCanEditProjectLeader,
      useWiki = project.getUseWiki,
      useFileSharing = project.getUseFileSharing,
      useDevAttributes = project.getUseDevAttributes,
      useResolvedForChart = project.getUseResolvedForChart,
      useWikiTreeView = project.getUseWikiTreeView,
      useOriginalImageSizeAtWiki = project.getUseOriginalImageSizeAtWiki,
      useSubversion = project.getUseSubversion,
      useGit = project.getUseGit
    )
  }
}
