package com.fortysevendeg.ninecardslauncher.process.user.impl

import cats.data.Xor
import com.fortysevendeg.ninecardslauncher.commons.NineCardExtensions._
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.commons.services.CatsService
import com.fortysevendeg.ninecardslauncher.commons.services.CatsService._
import com.fortysevendeg.ninecardslauncher.process.user._
import com.fortysevendeg.ninecardslauncher.process.user.models.Device
import com.fortysevendeg.ninecardslauncher.services.api.models.AndroidDevice
import com.fortysevendeg.ninecardslauncher.services.api.{ApiServices, InstallationResponse}
import com.fortysevendeg.ninecardslauncher.services.persistence._
import com.fortysevendeg.ninecardslauncher.services.persistence.models.{User => ServicesUser}

import scalaz.concurrent.Task

class UserProcessImpl(
  apiServices: ApiServices,
  persistenceServices: PersistenceServices)
  extends UserProcess
  with ImplicitsUserException
  with Conversions {

  private[this] val syncInstallationErrorMessage = "Installation not updated"

  private[this] val noActiveUserErrorMessage = "No active user"

  val emptyUserRequest = AddUserRequest(None, None, None, None, None, None, None, None, None, None)

  override def signIn(email: String, deviceName: String, token: String, permissions: Seq[String])(implicit context: ContextSupport) = {
    withActiveUser { id =>
      (for {
        androidId <- persistenceServices.getAndroidId
        device = Device(
          name = deviceName,
          deviceId = androidId,
          secretToken = token,
          permissions = permissions)
        loginResponse <- apiServices.login(email, toGoogleDevice(device))
        userDB <- persistenceServices.findUserById(FindUserByIdRequest(id)).resolveOption()
        updateUser = userDB.copy(
          email = Option(email),
          sessionToken = loginResponse.user.sessionToken,
          marketToken = Some(device.secretToken),
          deviceName = Some(device.name))
        _ <- persistenceServices.updateUser(toUpdateRequest(id, updateUser))
        _ <- syncInstallation(id, None, loginResponse.user.id, None)
      } yield SignInResponse(loginResponse.statusCode)).resolve[UserException]
    }
  }

  override def register(implicit context: ContextSupport) =
    context.getActiveUserId map { id =>
      (for {
        user <- checkOrAddUser(id)
        _ = if (id != user.id) context.setActiveUserId(user.id)
      } yield ()).resolve[UserException]
    } getOrElse {
      (for {
        user <- getFirstOrAddUser
        _ = context.setActiveUserId(user.id)
      } yield ()).resolve[UserException]
    }

  override def unregister(implicit context: ContextSupport) =
    withActiveUser { id =>
      val update = UpdateUserRequest(id, None, None, None, None, None, None, None, None, None, None)
      (for {
        _ <- syncInstallation(id, None, None, None)
        _ <- persistenceServices.updateUser(update)
      } yield ()).resolve[UserException]
    }

  override def getUser(implicit context: ContextSupport) =
    withActiveUser { id =>
      (for {
        user <- persistenceServices.findUserById(FindUserByIdRequest(id)).resolveOption()
      } yield toUser(user)).resolve[UserException]
    }

  override def updateUserDevice(
    deviceName: String,
    deviceCloudId: String,
    deviceToken: Option[String] = None)(implicit context: ContextSupport) =
    withActiveUser { id =>
      (for {
        user <- persistenceServices.findUserById(FindUserByIdRequest(id)).resolveOption()
        _ <- persistenceServices.updateUser(toUpdateRequest(
          id = id,
          user = user.copy(
            deviceName = Option(deviceName),
            deviceCloudId = Option(deviceCloudId),
            deviceToken = deviceToken orElse user.deviceToken)))
      } yield ()).resolve[UserException]
    }

  override def updateDeviceToken(
    deviceToken: String)(implicit context: ContextSupport) =
    withActiveUser { id =>
      (for {
        user <- persistenceServices.findUserById(FindUserByIdRequest(id)).resolveOption()
        _ <- persistenceServices.updateUser(toUpdateRequest(id, user.copy(deviceToken = Option(deviceToken))))
      } yield ()).resolve[UserException]
    }

  private[this] def withActiveUser[T](f: Int => CatsService[T])(implicit context: ContextSupport) =
    context.getActiveUserId map f getOrElse {
      CatsService(Task(Xor.Left(UserException(noActiveUserErrorMessage))))
    }

  private[this] def getFirstOrAddUser(implicit context: ContextSupport): CatsService[ServicesUser] =
    (for {
      maybeUsers <- persistenceServices.fetchUsers
      user <- maybeUsers.headOption map (user => CatsService(Task(Xor.right(user)))) getOrElse {
        persistenceServices.addUser(emptyUserRequest)
      }
    } yield user).resolve[UserException]

  private[this] def checkOrAddUser(id: Int)(implicit context: ContextSupport): CatsService[ServicesUser] =
    (for {
      maybeUser <- persistenceServices.findUserById(FindUserByIdRequest(id))
      user <- maybeUser map (user => CatsService(Task(Xor.right(user)))) getOrElse {
        persistenceServices.addUser(emptyUserRequest)
      }
    } yield user).resolve[UserException]

  private[this] def syncInstallation(
    id: Int,
    installationId: Option[String],
    userId: Option[String],
    deviceToken: Option[String])(implicit context: ContextSupport): CatsService[Int] =
    installationId map { id =>
      CatsService {
        apiServices.updateInstallation(
          id = id,
          deviceType = Some(AndroidDevice),
          deviceToken = deviceToken,
          userId = userId).value map {
          case Xor.Right(r) => Xor.Right(r.statusCode)
          // TODO - This need to be improved in ticket 9C-214
          case Xor.Left(_) => Xor.Left(UserException(syncInstallationErrorMessage))
        }
      }
    } getOrElse {
      (for {
        response <- apiServices.createInstallation(
          deviceType = Some(AndroidDevice),
          deviceToken = deviceToken,
          userId = userId)
      } yield response.statusCode).resolve[UserException]
    }

}
