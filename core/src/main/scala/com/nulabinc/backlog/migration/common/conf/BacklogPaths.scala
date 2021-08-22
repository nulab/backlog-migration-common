package com.nulabinc.backlog.migration.common.conf

import java.nio.file.{Path, Paths}
import java.util.Date

import better.files.{File => BetterFile}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, FileUtil}

/**
 * @author
 *   uchida
 */
class BacklogPaths(
    projectKey: String,
    basePath: Path = Paths.get("./backlog")
) {

  def outputPath: BetterFile = basePath

  def dbPath: Path = (outputPath / "data.db").path.toAbsolutePath

  def projectDirectoryPath(key: String): BetterFile =
    outputPath / "project" / key

  def projectJson: BetterFile = outputPath / "project.json"

  def groupsJson: BetterFile = outputPath / "groups.json"

  def projectUsersJson: BetterFile =
    outputPath / "project" / projectKey / "projectUsers.json"

  def customFieldSettingsJson: BetterFile =
    outputPath / "project" / projectKey / "customFieldSettings.json"

  def issueCategoriesJson: BetterFile =
    outputPath / "project" / projectKey / "issueCategories.json"

  def issueTypesJson: BetterFile =
    outputPath / "project" / projectKey / "issueTypes.json"

  def versionsJson: BetterFile =
    outputPath / "project" / projectKey / "versions.json"

  def wikiDirectoryPath: BetterFile =
    outputPath / "project" / projectKey / "wikis"

  def wikiJson(directory: String): BetterFile =
    wikiDirectoryPath / FileUtil.clean(directory) / "wiki.json"

  def wikiJson(path: BetterFile): BetterFile = path / "wiki.json"

  def wikiAttachmentDirectoryPath(directory: String): BetterFile =
    wikiDirectoryPath / FileUtil.clean(directory) / "attachment"

  def wikiAttachmentPath(path: BetterFile): BetterFile = path / "attachment"

  def wikiAttachmentPath(directory: String, fileName: String): BetterFile =
    wikiAttachmentDirectoryPath(FileUtil.clean(directory)) / FileUtil.clean(
      fileName
    )

  def issueDirectoryPath: BetterFile =
    outputPath / "project" / projectKey / "issues"

  def issueDirectoryPath(
      eventType: String,
      issueId: Long,
      created: Date,
      index: Int
  ): BetterFile =
    issueDirectoryPath / DateUtil.yyyymmddFormat(
      created
    ) / s"${created.getTime}-${issueId}-${eventType}-${index}"

  def issueJson(path: BetterFile): BetterFile = path / "issue.json"

  def issueAttachmentDirectoryPath(path: BetterFile): BetterFile =
    path / "attachment"

  def issueAttachmentPath(path: BetterFile, fileName: String): BetterFile =
    path / FileUtil.clean(fileName)

}
