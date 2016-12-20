package cards.nine.services.connectivity

import cards.nine.commons.services.TaskService.NineCardException

case class WifiServicesException(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message)
    with NineCardException {
  cause map initCause
}

trait ImplicitsWifiExceptions {
  implicit def wifiServicesException =
    (t: Throwable) => WifiServicesException(t.getMessage, Option(t))
}

case class BluetoothServicesException(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message)
    with NineCardException {
  cause map initCause
}

trait ImplicitsBluetoothExceptions {
  implicit def bluetoothServicesException =
    (t: Throwable) => BluetoothServicesException(t.getMessage, Option(t))
}
