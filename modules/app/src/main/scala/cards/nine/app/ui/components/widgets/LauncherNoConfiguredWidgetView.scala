package cards.nine.app.ui.components.widgets

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout.LayoutParams
import android.widget.{FrameLayout, ImageView}
import cards.nine.app.ui.commons.AsyncImageTweaks._
import cards.nine.app.ui.commons.{GenericUiContext, UiContext}
import cards.nine.app.ui.launcher.jobs.WidgetsJobs
import cards.nine.app.ui.commons.ops.TaskServiceOps._
import cards.nine.commons.ops.ColorOps._
import cards.nine.models.Widget
import macroid.extras.FrameLayoutTweaks._
import macroid.extras.ResourcesExtras._
import macroid.extras.ViewGroupTweaks._
import macroid.extras.ViewTweaks._
import com.fortysevendeg.ninecardslauncher.R
import macroid.FullDsl._
import macroid._

case class LauncherNoConfiguredWidgetView(id: Int, wCell: Int, hCell: Int, widget: Widget)(
    implicit contextWrapper: ContextWrapper,
    widgetJobs: WidgetsJobs)
    extends FrameLayout(contextWrapper.bestAvailable) {

  implicit lazy val uiContext: UiContext[Context] = GenericUiContext(getContext)

  val letter = "W"

  val paddingDefault = resGetDimensionPixelSize(R.dimen.padding_default)

  val stroke = resGetDimensionPixelSize(R.dimen.stroke_thin)

  val icon = (w[ImageView] <~
    vWrapContent <~
    ivSrcByPackageName(Some(widget.packageName), letter) <~
    flLayoutGravity(Gravity.CENTER)).get

  (this <~
    vBackgroundColor(Color.GRAY.alpha(.5f)) <~
    vgAddView(icon) <~
    On.click(Ui(widgetJobs.hostNoConfiguredWidget(widget).resolveAsync()))).run

  def addView(): Tweak[FrameLayout] =
    vgAddView(this, createParams())

  private[this] def createParams(): LayoutParams = {
    val (width, height) =
      (widget.area.spanX * wCell, widget.area.spanY * hCell)
    val (startX, startY) =
      (widget.area.startX * wCell, widget.area.startY * hCell)
    val params = new LayoutParams(width + stroke, height + stroke)
    val left   = paddingDefault + startX
    val top    = paddingDefault + startY
    params.setMargins(left, top, paddingDefault, paddingDefault)
    params
  }

}
