package com.fortysevendeg.ninecardslauncher.app.ui.components.layouts

import android.content.Context
import android.util.AttributeSet
import android.view.{LayoutInflater, View}
import android.widget.{FrameLayout, LinearLayout}
import com.fortysevendeg.macroid.extras.ImageViewTweaks._
import com.fortysevendeg.macroid.extras.TextTweaks._
import com.fortysevendeg.ninecardslauncher.commons._
import com.fortysevendeg.ninecardslauncher2.{R, TR, TypedFindView}
import macroid._

class StepsWorkspaces(context: Context, attr: AttributeSet, defStyleAttr: Int)
  extends AnimatedWorkSpaces[StepWorkSpaceWidgetsHolder, StepData](context, attr, defStyleAttr)
  with Contexts[View] {

  def this(context: Context) = this(context, javaNull, 0)

  def this(context: Context, attr: AttributeSet) = this(context, attr, 0)

  override def createEmptyView(): StepWorkSpaceWidgetsHolder = new StepWorkSpaceWidgetsHolder

  override def createView(viewType: Int): StepWorkSpaceWidgetsHolder = new StepWorkSpaceWidgetsHolder

  override def populateView(view: Option[StepWorkSpaceWidgetsHolder], data: StepData, viewType: Int, position: Int): Ui[_] =
    view match {
      case Some(v: StepWorkSpaceWidgetsHolder) => v.bind(data)
      case _ => Ui.nop
    }

}

case class StepData(image: Int, message: String)

class StepWorkSpaceWidgetsHolder(implicit contextWrapper: ContextWrapper)
  extends LinearLayout(contextWrapper.application)
  with TypedFindView {

  lazy val image = findView(TR.wizard_step_item_image)

  lazy val message = findView(TR.wizard_step_item_message)

  LayoutInflater.from(contextWrapper.application).inflate(R.layout.wizard_step, this)

  def bind(data: StepData): Ui[_] =
    (image <~ ivSrc(data.image)) ~
      (message <~ tvText(data.message))

}
