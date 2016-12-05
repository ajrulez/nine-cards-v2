package cards.nine.app.ui.launcher.holders

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.DragEvent._
import android.view.View
import android.view.ViewGroup.LayoutParams._
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageView
import cards.nine.app.ui.commons.CommonsTweak._
import cards.nine.app.ui.commons.ops.ViewOps._
import cards.nine.app.ui.commons.ops.WidgetsOps
import cards.nine.app.ui.commons.ops.WidgetsOps.Cell
import cards.nine.app.ui.components.drawables.DottedDrawable
import cards.nine.app.ui.components.layouts.{Dimen, LauncherWorkSpaceHolder}
import cards.nine.app.ui.components.models.LauncherMoment
import cards.nine.app.ui.components.widgets.LauncherWidgetView._
import cards.nine.app.ui.components.widgets.{LauncherNoConfiguredWidgetView, LauncherWidgetView}
import cards.nine.commons._
import cards.nine.models.Widget
import cards.nine.app.ui.commons.ops.TaskServiceOps._
import cards.nine.models.NineCardsTheme
import macroid.extras.ResourcesExtras._
import macroid.extras.ViewGroupTweaks._
import macroid.extras.ViewTweaks._
import com.fortysevendeg.ninecardslauncher.{R, TypedFindView}
import macroid.FullDsl._
import macroid._
import cards.nine.app.ui.launcher.LauncherActivity._
import cards.nine.app.ui.launcher.jobs.WidgetsJobs

class LauncherWorkSpaceMomentsHolder(context: Context, parentDimen: Dimen)(implicit widgetJobs: WidgetsJobs, theme: NineCardsTheme)
  extends LauncherWorkSpaceHolder(context)
  with Contexts[View]
  with TypedFindView {

  val ruleTag = "rule"

  case class WidgetStatuses(lastX: Option[Int] = None, lastY: Option[Int] = None) {
    def reset(): WidgetStatuses = copy(None, None)
  }

  var widgetStatuses = WidgetStatuses()

  val radius = resGetDimensionPixelSize(R.dimen.radius_default)

  val paddingDefault = resGetDimensionPixelSize(R.dimen.padding_default)

  val stroke = resGetDimensionPixelSize(R.dimen.stroke_thin)

  val drawable = {
    val s = 0 until 8 map (_ => radius.toFloat)
    val d = new ShapeDrawable(new RoundRectShape(s.toArray, javaNull, javaNull))
    d.getPaint.setColor(resGetColor(R.color.moment_workspace_background))
    d
  }

  def populate(moment: LauncherMoment): Ui[Any] =
     moment.momentType map (moment => Ui {
       widgetJobs.loadWidgetsForMoment(moment).resolveAsyncServiceOr(_ =>
         widgetJobs.navigationUiActions.showContactUsError())
     }) getOrElse clearWidgets

  def dragReorderController(action: Int, x: Float, y: Float): Unit = {
    val (sx, sy) = calculatePosition(x, y)
    ((action, sx, sy, widgetStatuses.lastX, widgetStatuses.lastY) match {
      case (ACTION_DRAG_LOCATION, currentX, currentY, lastX, lastY)
        if lastX.isEmpty || lastY.isEmpty || !lastX.contains(currentX) || !lastY.contains(currentY) =>
        widgetStatuses = widgetStatuses.copy(lastX = Option(currentX), lastY = Option(currentY))
        relocatedWidget(currentX, currentY)
      case (ACTION_DROP | ACTION_DRAG_ENDED, _, _, _, _) =>
        widgetStatuses = widgetStatuses.reset()
        resizeWidget()
      case _ => Ui.nop
    }).run
  }

  def relocatedWidget(newCellX: Int, newCellY: Int): Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if statuses.idWidget.contains(widgetView.widgetStatuses.widget.id) =>
      widgetView.moveView(newCellX, newCellY)
  }

  def resizeWidget(): Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if statuses.idWidget.contains(widgetView.widgetStatuses.widget.id) =>
      widgetView.activeResize()
  }

  def reloadSelectedWidget: Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if statuses.idWidget.contains(widgetView.widgetStatuses.widget.id) => widgetView.activeSelected()
    case widgetView: LauncherWidgetView => widgetView.deactivateSelected()
  }

  def resizeCurrentWidget: Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if statuses.idWidget.contains(widgetView.widgetStatuses.widget.id) => widgetView.activeResizing()
    case widgetView: LauncherWidgetView => widgetView.deactivateSelected()
  }

  def moveCurrentWidget: Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if statuses.idWidget.contains(widgetView.widgetStatuses.widget.id) => widgetView.activeMoving()
    case widgetView: LauncherWidgetView => widgetView.deactivateSelected()
  }

  def resizeWidgetById(id: Int, increaseX: Int, increaseY: Int): Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if widgetView.widgetStatuses.widget.id == id =>
      (for {
        cell <- widgetView.getField[Cell](cellKey)
        widget <- widgetView.getField[Widget](widgetKey)
      } yield {
        val newWidget = widget.copy(area = widget.area.copy(
          spanX = widget.area.spanX + increaseX,
          spanY = widget.area.spanY + increaseY))
        (widgetView <~ saveInfoInTag(cell, newWidget)) ~
          widgetView.adaptSize(newWidget)
      }) getOrElse Ui.nop
  }

  def moveWidgetById(id: Int, displaceX: Int, displaceY: Int): Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if widgetView.widgetStatuses.widget.id == id =>
      (for {
        widget <- widgetView.getField[Widget](widgetKey)
      } yield {
        val newWidget = widget.copy(area = widget.area.copy(
          startX = widget.area.startX + displaceX,
          startY = widget.area.startY + displaceY))
        (widgetView <~ vAddField(widgetKey, newWidget)) ~
          widgetView.adaptSize(newWidget)
      }) getOrElse Ui.nop
  }

  def addWidget(widgetView: AppWidgetHostView, cell: Cell, widget: Widget): Ui[Any] = {
    val launcherWidgetView = (LauncherWidgetView(widget, widgetView) <~ saveInfoInTag(cell, widget)).get
    this <~ launcherWidgetView.addView(cell, widget)
  }

  def addNoConfiguredWidget(wCell: Int, hCell: Int, widget: Widget): Ui[Any] = {
    val noConfiguredWidgetView = LauncherNoConfiguredWidgetView(widget.id, wCell, hCell, widget)
    this <~ noConfiguredWidgetView.addView()
  }

  def addReplaceWidget(widgetView: AppWidgetHostView, wCell: Int, hCell: Int, widget: Widget): Ui[Any] = {
    val cell = Cell(widget.area.spanX, widget.area.spanY, wCell, hCell)
    (this <~ Transformer {
      case i: LauncherNoConfiguredWidgetView if i.id == widget.id => this <~ vgRemoveView(i)
    }) ~ addWidget(widgetView, cell, widget)
  }

  def clearWidgets: Ui[Any] = this <~ vgRemoveAllViews

  def unhostWiget(id: Int): Ui[Any] = this <~ Transformer {
    case widgetView: LauncherWidgetView if widgetView.widgetStatuses.widget.id == id => this <~ vgRemoveView(widgetView)
  }

  def createRules: Ui[Any] = {
    val spaceWidth = (getWidth - (paddingDefault * 2)) / WidgetsOps.columns
    val spaceHeight = (getHeight - (paddingDefault * 2)) / WidgetsOps.rows

    def createView(horizontal: Boolean = true) =
      (w[ImageView] <~
        vWrapContent <~
        vTag(ruleTag) <~
        vBackground(new DottedDrawable(horizontal))).get

    def horizontalRules(pos: Int) = {
      val params = new LayoutParams(MATCH_PARENT, stroke)
      params.leftMargin = paddingDefault
      params.rightMargin = paddingDefault
      params.topMargin = (pos * spaceHeight) + paddingDefault
      vgAddViewByIndexParams(createView(), 0, params)
    }

    def verticalRules(pos: Int) = {
      val params = new LayoutParams(stroke, MATCH_PARENT)
      params.topMargin = paddingDefault
      params.bottomMargin = paddingDefault
      params.leftMargin = (pos * spaceWidth) + paddingDefault
      vgAddViewByIndexParams(createView(horizontal = false), 0, params)
    }

    val tweaks = ((1 until WidgetsOps.rows) map horizontalRules) ++ ((1 until WidgetsOps.columns) map verticalRules)
    val uis = tweaks map (tweak => this <~ tweak)
    Ui.sequence(uis: _*)
  }

  def removeRules(): Ui[Any] = this <~ Transformer {
    case i: ImageView if i.getTag == ruleTag => this <~ vgRemoveView(i)
  }

  private[this] def saveInfoInTag(cell: Cell, widget: Widget) =
    vAddField(cellKey, cell) +
      vAddField(widgetKey, widget)

  private[this] def calculatePosition(x: Float, y: Float) = {
    val w = getWidth
    val h = getHeight
    val spaceX = w / 5
    val spaceY = h / 5
    val sx = (x / spaceX).toInt
    val sy = (y / spaceY).toInt
    (sx, sy)
  }

}

sealed trait Arrow

case object ArrowUp extends Arrow
case object ArrowDown extends Arrow
case object ArrowLeft extends Arrow
case object ArrowRight extends Arrow