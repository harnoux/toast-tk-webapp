package controllers.mongo.users

import scala.util.{Failure, Success}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONArray}

import java.security.SecureRandom

case class UserCollection(collection: BSONCollection){

  def AuthenticateUser(user : InspectedUser) : Option[User] = {
    var isAuthenticated = false
    val query = BSONDocument("login" -> user.login, "password" -> user.password)
    var authPersonOpt: Option[User]  = None;
    var token:Option[String] = None ;
    val userFuture =
    collection.
    find(query). 
    cursor[User]().
    collect[List]()
    Await.result(userFuture.map { users =>
      for(person <- users) {
        token = Some(BearerTokenGenerator.generateToken)
        val authPerson = User(person.id,
          person.login,
          person.password,
          person.firstName,
          person.lastName,
          person.email,
          person.teams,
          token,
          true,
          None)
        authPersonOpt = Some(authPerson)
        println(s"dataobj Token ----> ${authPersonOpt}")
        saveUser(authPerson)
        val firstName = authPerson.firstName
        isAuthenticated = true
        println(s"found $firstName $isAuthenticated")
        authPersonOpt
      }
    }, 5 seconds)
    println(s"just here $isAuthenticated")
    authPersonOpt
  }

/*  def saveUser(user: User) {
    val collection = open_collection("users")
         println(s"[+] successfully gooottt user $user !")

    user.id match {
      case None => collection.insert(user).onComplete {
        case Failure(e) => throw e
        case Success(_) => println("[+] successfully inserted ${user.id} and $user !")
      }
      case Some(_) => collection.update(BSONDocument("_id" -> BSONObjectID(user.id.get)), user, upsert=true).onComplete {
        case Failure(e) => throw e
        case Success(_) => println("successfully saved user !")
      }
    }
  }*/

  def saveUser(user: User)  : Future[Boolean] = {
    println(s"[+] successfully gooottt user $user !")

    user.id match {
      case None => {
         Future{false} //looks like not reached
       }
       case _ => findUserBy(BSONDocument(
        "$or" -> BSONArray(
          BSONDocument(
            "_id" -> BSONDocument("$ne" -> BSONObjectID(user.id.get)),
            "login" -> user.login
            ),
          BSONDocument(
            "_id" -> BSONDocument("$ne" -> BSONObjectID(user.id.get)),
            "email" -> user.email
            )
          )
        )
       ).map{
        case None => {
          collection.insert(user).onComplete {
            case Failure(e) => throw e
            case Success(_) => println("[+] successfully inserted ${user.id} and $user !")
          }
          true
        }
        case Some(user) => {
          println(s"[+] successfully found ${user.id} and $user !")
          false
        }
      }
    }
  }

  def disconnectUser(id : String) : Future[Boolean] = {
    findUserBy(
          BSONDocument(
            "_id" -> BSONObjectID(id)
            )
       ).map{
        case None => {
          println(s"[+] User not found, could not disconnect properly !")
          false
        }
        case Some(user) => {
          println(s"[+] disconnecting ${id} ${user.id} and $user !")
          collection.update(BSONDocument("_id" -> BSONObjectID(id)), 
              BSONDocument(
                "$set" -> BSONDocument(
                     "isActive" -> false
                  )
              ),
              upsert=false
            ).onComplete {
            case Failure(e) => throw e
            case Success(_) => println("successfully saved configuration !")
          }
          true
        }
      }
  }

  def findUserBy(query: BSONDocument): Future[Option[User]] = {
    collection.find(query).one[User]
  }

  def getAllUsers() : Future[List[User]] ={
    val query = BSONDocument()
    val users = collection.find(query).cursor[User]().collect[List]()
    users
  }

  object BearerTokenGenerator {
  
  val TOKEN_LENGTH = 32
  val TOKEN_CHARS = 
     "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()
    
  def generateToken:String =  
    generateToken(TOKEN_LENGTH)   
  
  def generateToken(tokenLength: Int): String =
    if(tokenLength == 0) "" else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) + 
     generateToken(tokenLength - 1)
  
}

}