package cards.nine.app.ui.launcher.jobs

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.{Fragment, FragmentManager}
import android.support.v4.view.GravityCompat
import android.view.View
import cards.nine.app.ui.commons.ActivityFindViews
import cards.nine.app.ui.commons.actions.BaseActionFragment
import cards.nine.app.ui.commons.ops.ViewOps._
import cards.nine.app.ui.components.layouts.tweaks.TabsViewTweaks._
import cards.nine.app.ui.components.layouts.tweaks.LauncherWorkSpacesTweaks._
import cards.nine.app.ui.components.models.LauncherData
import cards.nine.app.ui.components.widgets.ContentView
import cards.nine.models.types.NineCardsMoment
import com.fortysevendeg.macroid.extras.FragmentExtras._
import com.fortysevendeg.macroid.extras.ResourcesExtras._
import com.fortysevendeg.ninecardslauncher.{R, TR}
import macroid._

class LauncherDOM(activity: Activity) {

  import ActivityFindViews._

  val nameActionFragment = "action-fragment"

  lazy val foreground = findView(TR.launcher_foreground).run(activity)

  lazy val appsMoment = findView(TR.launcher_apps_moment).run(activity)

  lazy val drawerLayout = findView(TR.launcher_drawer_layout).run(activity)

  lazy val navigationView = findView(TR.launcher_navigation_view).run(activity)

  lazy val menuName = findView(TR.menu_name).run(activity)

  lazy val menuEmail = findView(TR.menu_email).run(activity)

  lazy val menuAvatar = findView(TR.menu_avatar).run(activity)

  lazy val menuCover = findView(TR.menu_cover).run(activity)

  lazy val loading = findView(TR.launcher_loading).run(activity)

  lazy val root = findView(TR.launcher_root).run(activity)

  lazy val dockAppsPanel = findView(TR.launcher_dock_apps_panel).run(activity)

  lazy val content = findView(TR.launcher_content).run(activity)

  lazy val workspaces = findView(TR.launcher_work_spaces).run(activity)

  lazy val workspacesEdgeLeft = findView(TR.launcher_work_spaces_edge_left).run(activity)

  lazy val workspacesEdgeRight = findView(TR.launcher_work_spaces_edge_right).run(activity)

  lazy val paginationPanel = findView(TR.launcher_pagination_panel).run(activity)

  lazy val topBarPanel = findView(TR.launcher_top_bar_panel).run(activity)

  lazy val editWidgetsTopPanel = findView(TR.launcher_edit_widgets_top_panel).run(activity)

  lazy val editWidgetsBottomPanel = findView(TR.launcher_edit_widgets_bottom_panel).run(activity)

  lazy val collectionActionsPanel = findView(TR.launcher_collections_actions_panel).run(activity)

  lazy val actionFragmentContent = findView(TR.action_fragment_content).run(activity)

  lazy val menuCollectionRoot = findView(TR.menu_collection_root).run(activity)

  lazy val menuWorkspaceContent = findView(TR.menu_workspace_content).run(activity)

  lazy val menuLauncherContent = findView(TR.menu_launcher_content).run(activity)

  lazy val menuLauncherWallpaper = findView(TR.menu_launcher_wallpaper).run(activity)

  lazy val menuLauncherWidgets = findView(TR.menu_launcher_widgets).run(activity)

  lazy val menuLauncherSettings = findView(TR.menu_launcher_settings).run(activity)

  lazy val fragmentContent = findView(TR.action_fragment_content).run(activity)

  lazy val appDrawerMain = findView(TR.launcher_app_drawer).run(activity)

  lazy val drawerContent = findView(TR.launcher_drawer_content).run(activity)

  lazy val scrollerLayout = findView(TR.launcher_drawer_scroller_layout).run(activity)

  lazy val paginationDrawerPanel = findView(TR.launcher_drawer_pagination_panel).run(activity)

  lazy val recycler = findView(TR.launcher_drawer_recycler).run(activity)

  lazy val tabs = findView(TR.launcher_drawer_tabs).run(activity)

  lazy val pullToTabsView = findView(TR.launcher_drawer_pull_to_tabs).run(activity)

  lazy val screenAnimation = findView(TR.launcher_drawer_swipe_animated).run(activity)

  lazy val searchBoxView = findView(TR.launcher_search_box_content).run(activity)

  def getWorksSpacesCount: Int = workspaces.getWorksSpacesCount

  def getData: Seq[LauncherData] = workspaces.data

  def getCurrentMomentType: Option[NineCardsMoment] = getData.headOption flatMap (_.moment) flatMap (_.momentType)

  def isMenuVisible: Boolean = drawerLayout.isDrawerOpen(GravityCompat.START)

  def isCollectionMenuVisible: Boolean = workspaces.workSpacesStatuses.openedMenu

  def isDrawerTabsOpened: Boolean = (tabs ~> isOpened).get

  def getStatus: Option[String] = recycler.getType

  def getTypeView: Option[ContentView] = Option(recycler.statuses.contentView)

  def getItemsCount: Int = Option(recycler.getAdapter) map (_.getItemCount) getOrElse 0

  def getDrawerWidth: Int = drawerContent.getWidth

  def isDrawerVisible = drawerContent.getVisibility == View.VISIBLE

  def isEmptyCollections: Boolean = (workspaces ~> lwsEmptyCollections).get

  def isWorkspaceScrolling: Boolean = workspaces.animatedWorkspaceStatuses.isScrolling

  def createBundle(maybeView: Option[View], color: Int, map: Map[String, String] = Map.empty)
    (implicit contextWrapper: ContextWrapper): Bundle = {
    val sizeIconWorkSpaceMenuItem = resGetDimensionPixelSize(R.dimen.size_workspace_menu_item)
    val (startX: Int, startY: Int) = maybeView map (_.calculateAnchorViewPosition) getOrElse(0, 0)
    val (startWX: Int, startWY: Int) = workspaces.calculateAnchorViewPosition
    val (endPosX: Int, endPosY: Int) = (startWX + workspaces.animatedWorkspaceStatuses.dimen.width / 2, startWY + workspaces.animatedWorkspaceStatuses.dimen.height / 2)
    val x = startX + (sizeIconWorkSpaceMenuItem / 2)
    val y = startY + (sizeIconWorkSpaceMenuItem / 2)
    val args = new Bundle()
    args.putInt(BaseActionFragment.sizeIcon, sizeIconWorkSpaceMenuItem)
    args.putInt(BaseActionFragment.startRevealPosX, x)
    args.putInt(BaseActionFragment.startRevealPosY, y)
    args.putInt(BaseActionFragment.endRevealPosX, endPosX)
    args.putInt(BaseActionFragment.endRevealPosY, endPosY)
    map foreach {
      case (key, value) => args.putString(key, value)
    }
    args.putInt(BaseActionFragment.colorPrimary, color)
    args
  }

  def isActionShowed(implicit fragmentManagerContext: FragmentManagerContext[Fragment, FragmentManager]): Boolean = findFragmentByTag(nameActionFragment).isDefined

  def getFragment(implicit fragmentManagerContext: FragmentManagerContext[Fragment, FragmentManager]): Option[BaseActionFragment] =
    findFragmentByTag[BaseActionFragment](nameActionFragment)

}