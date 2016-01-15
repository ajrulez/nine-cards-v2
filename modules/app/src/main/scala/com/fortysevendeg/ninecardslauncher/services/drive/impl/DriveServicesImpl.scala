package com.fortysevendeg.ninecardslauncher.services.drive.impl

import java.io.{InputStream, OutputStreamWriter}

import com.fortysevendeg.ninecardslauncher.commons._
import com.fortysevendeg.ninecardslauncher.commons.services.Service
import com.fortysevendeg.ninecardslauncher.services.drive.{DriveResourceNotAvailable, DriveRateLimitExceeded, DriveSigInRequired}
import com.fortysevendeg.ninecardslauncher.services.drive.impl.Extensions._
import com.fortysevendeg.ninecardslauncher.services.drive.models.DriveServiceFile
import com.fortysevendeg.ninecardslauncher.services.drive.{Conversions, DriveServicesException, DriveServices}
import com.google.android.gms.common.api.{CommonStatusCodes, GoogleApiClient, PendingResult, Result}
import com.google.android.gms.drive._
import com.google.android.gms.drive.metadata.CustomPropertyKey
import com.google.android.gms.drive.query.{Filters, Query}
import rapture.core
import rapture.core.{Answer, Errata}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}
import scalaz.concurrent.Task
import scalaz.Scalaz._

class DriveServicesImpl(client: GoogleApiClient)
  extends DriveServices
  with Conversions {

  private[this] val customFileType = "FILE_TYPE"

  private[this] val customFileId = "FILE_ID"

  private[this] def propertyFileType = new CustomPropertyKey(customFileType, CustomPropertyKey.PRIVATE)

  private[this] def propertyFileId = new CustomPropertyKey(customFileId, CustomPropertyKey.PRIVATE)

  def listFiles(maybeFileType: Option[String]) = {
    val maybeQuery = maybeFileType map { fileType =>
      new Query.Builder()
        .addFilter(Filters.eq(propertyFileType, fileType))
        .build()
    }
    searchFiles(maybeQuery)(_ map toGoogleDriveFile)
  }

  def findFile(fileId: String) = {
    val query = new Query.Builder()
      .addFilter(Filters.eq(propertyFileId, fileId))
      .build()
    searchFiles(query.some)(_.headOption map toGoogleDriveFile)
  }

  def readFile(driveId: String) =
    openDriveFile(driveId) { driveContentsResult =>
      val contents = driveContentsResult.getDriveContents
      Answer(scala.io.Source.fromInputStream(contents.getInputStream).mkString)
    }

  def openFile(driveId: String) =
    openDriveFile(driveId) { driveContentsResult =>
      val contents = driveContentsResult.getDriveContents
      Answer(contents.getInputStream)
    }

  def createFile(title: String, content: String, fileId: String, fileType: String, mimeType: String) =
    for {
      file <- createNewFile(title, fileId, fileType, mimeType)
      update <- updateFile(file.driveId, content)
    } yield update

  def createFile(title: String, content: InputStream, fileId: String, fileType: String, mimeType: String) =
    for {
      file <- createNewFile(title, fileId, fileType, mimeType)
      update <- updateFile(file.driveId, content)
    } yield update

  def updateFile(driveId: String, content: String) =
    updateFile(driveId, _.write(content))

  def updateFile(driveId: String, content: InputStream) =
    updateFile(
      driveId,
      writer => Iterator
        .continually(content.read)
        .takeWhile(_ != -1)
        .foreach(writer.write))

  private[this] def searchFiles[R](query: Option[Query])(f: (Seq[Metadata]) => R) = Service {
    Task {
      val request = query match {
        case Some(q) => appFolder.queryChildren(client, q)
        case _ => appFolder.listChildren(client)
      }
      request.withResult { r =>
        Answer(f(r.getMetadataBuffer.iterator().toSeq))
      }
    }
  }

  private[this] def appFolder = Drive.DriveApi.getAppFolder(client)

  private[this] def createNewFile(title: String, fileId: String, fileType: String, mimeType: String) = Service {
    Task {
      Drive.DriveApi
        .newDriveContents(client)
        .withResult { r =>
          val changeSet = new MetadataChangeSet.Builder()
            .setTitle(title)
            .setMimeType(mimeType)
            .setCustomProperty(propertyFileId, fileId)
            .setCustomProperty(propertyFileType, fileType)
            .build()

          appFolder
            .createFile(client, changeSet, r.getDriveContents)
            .withResult(nr => Answer(toGoogleDriveFile(title, nr.getDriveFile)))
        }

    }
  }

  private[this] def updateFile(driveId: String, f: (OutputStreamWriter) => Unit) =
    openDriveFile(driveId) { driveContentsResult =>
      val contents = driveContentsResult.getDriveContents
      val writer = new OutputStreamWriter(contents.getOutputStream)
      f(writer)
      writer.close()
      contents.commit(client, javaNull).withResult(_ => Answer())
    }

  private[this] def openDriveFile[R](driveId: String)(f: (DriveApi.DriveContentsResult) => core.Result[R, DriveServicesException]) = Service {
    Task {
      Drive.DriveApi
        .fetchDriveId(client, driveId)
        .withResult { result =>
          result
            .getDriveId.asDriveFile()
            .open(client, DriveFile.MODE_READ_ONLY, javaNull)
            .withResult(f(_))
        }
    }
  }

}

object Extensions {

  implicit class PendingResultOps[T <: Result](pendingResult: PendingResult[T]) {

    def withResult[R](f: (T) => core.Result[R, DriveServicesException]): core.Result[R, DriveServicesException] = {
      val result = pendingResult.await()
      if (result.getStatus.isSuccess) {
        Try(f(result)) match {
          case Success(r) => r
          case Failure(e) => Errata(DriveServicesException(e.getMessage, cause = Some(e)))
        }
      } else {
        Errata(DriveServicesException(
          googleDriveError = statusCodeToError(result.getStatus.getStatusCode),
          message = result.getStatus.getStatusMessage))
      }
    }

    private[this] def statusCodeToError(statusCode: Int) = statusCode match {
      case CommonStatusCodes.SIGN_IN_REQUIRED => DriveSigInRequired.some
      case DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED => DriveRateLimitExceeded.some
      case DriveStatusCodes.DRIVE_RESOURCE_NOT_AVAILABLE => DriveResourceNotAvailable.some
      case _ => None
    }

  }

}
