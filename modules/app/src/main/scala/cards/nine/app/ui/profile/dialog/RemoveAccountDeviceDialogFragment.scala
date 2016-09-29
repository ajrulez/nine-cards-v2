package cards.nine.app.ui.profile.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import cards.nine.app.ui.profile.ProfilePresenter
import cards.nine.commons._
import com.fortysevendeg.ninecardslauncher.R
import macroid.ContextWrapper

class RemoveAccountDeviceDialogFragment(resourceId: String)(implicit contextWrapper: ContextWrapper, profilePresenter: ProfilePresenter)
  extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {

    new AlertDialog.Builder(getActivity).
      setMessage(R.string.removeAccountSyncMessage).
      setPositiveButton(android.R.string.ok, new OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = profilePresenter.deleteDevice(resourceId)
      }).
      setNegativeButton(android.R.string.cancel, javaNull).
      create()
  }

}
