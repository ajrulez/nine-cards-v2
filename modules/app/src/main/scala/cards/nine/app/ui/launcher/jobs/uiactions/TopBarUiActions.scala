/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cards.nine.app.ui.launcher.jobs.uiactions

import android.support.v4.app.{Fragment, FragmentManager}
import cards.nine.app.ui.commons.UiContext
import cards.nine.app.ui.commons.ops.UiOps._
import cards.nine.app.ui.components.layouts.tweaks.TopBarLayoutTweaks._
import cards.nine.app.ui.components.models.{CollectionsWorkSpace, LauncherData, WorkSpaceType}
import cards.nine.app.ui.launcher.LauncherActivity._
import cards.nine.commons.services.TaskService.TaskService
import cards.nine.models.NineCardsTheme
import macroid.{ActivityContextWrapper, FragmentManagerContext, Tweak}

class TopBarUiActions(val dom: LauncherDOM)(
    implicit activityContextWrapper: ActivityContextWrapper,
    fragmentManagerContext: FragmentManagerContext[Fragment, FragmentManager],
    uiContext: UiContext[_]) {

  implicit lazy val launcherJobs = createLauncherJobs

  implicit lazy val navigationJobs = createNavigationJobs

  implicit def theme: NineCardsTheme = statuses.theme

  def initialize(): TaskService[Unit] =
    (dom.topBarPanel <~ tblInit(CollectionsWorkSpace)).toService()

  def loadBar(data: Seq[LauncherData]): TaskService[Unit] = {
    val momentType = data.headOption.flatMap(_.moment).flatMap(_.momentType)
    (dom.topBarPanel <~
      tblReloadByType(CollectionsWorkSpace) <~
      (momentType map tblReloadMoment getOrElse Tweak.blank)).toService(Option("loadBar"))
  }

  def reloadMomentTopBar(): TaskService[Unit] = {
    val momentType =
      dom.getData.headOption.flatMap(_.moment).flatMap(_.momentType)
    (dom.topBarPanel <~ (momentType map tblReloadMoment getOrElse Tweak.blank)).toService()
  }

  def reloadTopBar(): TaskService[Unit] =
    (dom.topBarPanel <~ tblReload).toService()

}
