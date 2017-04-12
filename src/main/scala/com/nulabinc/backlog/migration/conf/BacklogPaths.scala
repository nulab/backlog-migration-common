package com.nulabinc.backlog.migration.conf

import com.nulabinc.backlog.migration.utils.FileUtil

import scalax.file.Path

/**
  * @author uchida
  */
class BacklogPaths(projectKey: String) {

  def outputPath: Path = Path(".") / "backlog"

  def projectDirectoryPath(key: String): Path = outputPath / "project" / key

  def projectJson: Path = outputPath / "project.json"

  def groupsJson: Path = outputPath / "groups.json"

  def projectUsersJson: Path = outputPath / "project" / projectKey / "projectUsers.json"

  def customFieldSettingsJson: Path = outputPath / "project" / projectKey / "customFieldSettings.json"

  def issueCategoriesJson: Path = outputPath / "project" / projectKey / "issueCategories.json"

  def issueTypesJson: Path = outputPath / "project" / projectKey / "issueTypes.json"

  def versionsJson: Path = outputPath / "project" / projectKey / "versions.json"

  def wikiDirectoryPath: Path = outputPath / "project" / projectKey / "wikis"

  def wikiJson(directory: String): Path = wikiDirectoryPath / FileUtil.clean(directory) / "wiki.json"

  def wikiJson(path: Path): Path = path / "wiki.json"

  def wikiAttachmentDirectoryPath(directory: String): Path = wikiDirectoryPath / FileUtil.clean(directory) / "attachment"

  def wikiAttachmentPath(path: Path): Path = path / "attachment"

  def wikiAttachmentPath(directory: String, fileName: String): Path =
    wikiAttachmentDirectoryPath(FileUtil.clean(directory)) / FileUtil.clean(fileName)

  def issueDirectoryPath: Path = outputPath / "project" / projectKey / "issues"

  def issueDirectoryPath(date: String, label: String): Path = issueDirectoryPath / date / label

  def issueJson(path: Path): Path = path / "issue.json"

  def issueAttachmentDirectoryPath(path: Path): Path = path / "attachment"

  def issueAttachmentPath(path: Path, fileName: String): Path = path / FileUtil.clean(fileName)

}
