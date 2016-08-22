package com.fortysevendeg.ninecardslauncher.services.wifi.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.fortysevendeg.ninecardslauncher.commons.XorCatchAll
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.commons.services.CatsService
import com.fortysevendeg.ninecardslauncher.services.wifi.{ImplicitsWifiExceptions, WifiServices, WifiServicesException}

import scala.util.Try
import scalaz.concurrent.Task

class WifiServicesImpl
  extends WifiServices
  with ImplicitsWifiExceptions {

  override def getCurrentSSID(implicit contextSupport: ContextSupport) = CatsService {
    Task {
      XorCatchAll[WifiServicesException] {
        val connManager = getConnectivityManager
        val networkInfo = connManager map (_.getActiveNetworkInfo())

        networkInfo match {
          case Some(n) if n.isConnected && n.getType == ConnectivityManager.TYPE_WIFI && n.getExtraInfo != "" =>
            Option(n.getExtraInfo)
          case _ => None
        }
      }
    }
  }

  override def getConfiguredNetworks(implicit contextSupport: ContextSupport) = Service {
    Task {
      XorCatchAll[WifiServicesException] {
        import scala.collection.JavaConversions._
        val wifiManager = getWifiManager
        val networks = wifiManager map (_.getConfiguredNetworks.toList) getOrElse List.empty
        networks map (_.SSID.replace("\"", "")) sortWith(_.toLowerCase() < _.toLowerCase())
      }
    }
  }


  protected def getConnectivityManager(implicit contextSupport: ContextSupport): Option[ConnectivityManager] =
    contextSupport.context.getSystemService(Context.CONNECTIVITY_SERVICE) match {
      case conn : ConnectivityManager => Some(conn)
      case _ => None
    }

  protected def getWifiManager(implicit contextSupport: ContextSupport): Option[WifiManager] =
    contextSupport.context.getSystemService(Context.WIFI_SERVICE) match {
      case manager : WifiManager => Some(manager)
      case _ => None
    }

}
